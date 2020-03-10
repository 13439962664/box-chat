package com.box.chat.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.box.chat.pojo.ChatDto;
import com.box.chat.pojo.ChatMessage;
import com.box.chat.pojo.ChatUser;
import com.box.chat.service.ChatWebSocketService;
import com.box.utils.ApplicationContextHolder;

@ServerEndpoint("/chat/{userType}/{userId}")
@Component
public class ChatWebSocketServer {

	private static final String reloadOnlineUserInfo = "0/10 * * * * ?";
	private static final String onlineUserHeartbeat = "0/5 * * * * ?";
	private static final String pullUnreadMessages = "0/7 * * * * ?";
	private static final Logger log = LoggerFactory.getLogger(ChatWebSocketServer.class);

	/** 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。 */
//    private static int onlineCount = 0;
	/** concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。 */
	public static ConcurrentHashMap<String, ChatWebSocketServer> webSocketMap = new ConcurrentHashMap<>();
	/** 与某个客户端的连接会话，需要通过它来给客户端发送数据 */
	private Session session;
	private ChatUser user;
	
	private static ChatWebSocketService chatWebSocketService;
	
	private ChatWebSocketService getChatWebSocketService() {
		if (chatWebSocketService == null) {
			chatWebSocketService = ApplicationContextHolder.getBean("chatWebSocketService");
		}
		return chatWebSocketService;
	}

	/**
	 * 连接建立成功调用的方法
	 */
	@OnOpen
	public void onOpen(Session session, @PathParam("userType") String userType, @PathParam("userId") String userId) {
		this.session = session;
		this.user = new ChatUser(userId, userType);
		ChatUser chatUser = new ChatUser(userId,userType);
		String userTypeAndId = String.format("%s:%s", userType, userId);
		if (webSocketMap.containsKey(userTypeAndId)) {
			webSocketMap.remove(userTypeAndId);
			webSocketMap.put(userTypeAndId, this);
			// 加入set中
		} else {
			webSocketMap.put(userTypeAndId, this);
			// 加入set中
			getChatWebSocketService().addOnlineCount(chatUser);
			// 在线数加1
		}
		log.info("用户连接:" + userTypeAndId + ",当前在线人数为:" + getChatWebSocketService().getOnlineCount(userType));
		try {
//			sendMessageNow("连接成功");
			getChatWebSocketService().loadChatOnlineUsersCore(Arrays.asList(chatUser),false);
			getChatWebSocketService().pullUnreadMessagesCore(null,chatUser);
		} catch (IOException e) {
			log.error("用户:" + userId + ",网络异常!!!!!!");
		}
	}

	/**
	 * 收到客户端消息后调用的方法
	 *
	 * @param message 客户端发送过来的消息
	 */
	@OnMessage
	public void onMessage(String requestStr, Session session) throws Exception{
		ChatDto<ChatMessage,ChatMessage> dto = (ChatDto<ChatMessage,ChatMessage>)JSONObject.parseObject(requestStr, new TypeReference<ChatDto<ChatMessage,ChatMessage>>(){});
		
		switch(dto.getRequest().getAction()) {
			case "pullMessage":
				getChatWebSocketService().pullMessage(dto,this.user);
				break;
			case "sendMessage":
				getChatWebSocketService().sendMessage(dto,this.user);
				break;
		}
	}
	
	/**
	 * 实现服务器主动推送
	 */
	public void sendMessageNow(String message) throws IOException {
		this.session.getBasicRemote().sendText(message);
	}
	
	@Scheduled(cron = pullUnreadMessages)
	private void pullUnreadMessages() throws IOException {
		getChatWebSocketService().pullUnreadMessagesCore(null,null);
	}
	
	
	
	@Scheduled(cron = reloadOnlineUserInfo)
	private void loadChatOnlineUsers() throws IOException {
		getChatWebSocketService().loadChatOnlineUsersCore(null,true);
	}
	
	/**
	 * 连接关闭调用的方法
	 */
	@OnClose
	public void onClose() {
		String userTypeAndId = String.format("%s:%s", this.user.getType(), this.user.getId());
		if (webSocketMap.containsKey(userTypeAndId)) {
			webSocketMap.remove(userTypeAndId);
			// 从set中删除
			getChatWebSocketService().subOnlineCount(this.user);
		}
		log.info("用户退出:" + userTypeAndId + ",当前在线人数为:" + getChatWebSocketService().getOnlineCount(this.user.getType()));
	}

	/**
	 *
	 * @param session
	 * @param error
	 */
	@OnError
	public void onError(Session session, Throwable error) {
		log.error("用户错误:" + this.user.getId() + ",原因:" + error.getMessage());
		error.printStackTrace();
	}

	@Scheduled(cron = onlineUserHeartbeat)
	private void configureTasks() {
		Enumeration<String> keys = webSocketMap.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			String[] keyss = key.split(":");
			getChatWebSocketService().getRedisUtil().expire(String.format(ChatWebSocketService.onlineUser, keyss[0], keyss[1]), ChatWebSocketService.onlineUserInfoSaveSecond);
		}
	}
}
