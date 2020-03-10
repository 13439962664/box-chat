package com.box.chat.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.box.chat.controller.ChatWebSocketServer;
import com.box.chat.pojo.ChatDto;
import com.box.chat.pojo.ChatMessage;
import com.box.chat.pojo.ChatMessageTypeEnum;
import com.box.chat.pojo.ChatOnline;
import com.box.chat.pojo.ChatRequest;
import com.box.chat.pojo.ChatResponse;
import com.box.chat.pojo.ChatUser;
import com.box.chat.pojo.ChatUserTypeEnum;
import com.box.utils.ApplicationContextHolder;
import com.box.utils.RedisUtil;
import com.box.utils.lock.RedisLock;

@Component
public class ChatWebSocketService {

	public static final String onlineUser = "crm:online:online:%s:%s";
	private static final String justOnlineUser = "crm:online:justonline:%s:%s";
	private static final String justOfflineUser = "crm:online:justoffline:%s:%s";
	private static final String messageAddress = "crm:message:fromuser:%s:%s:touser:%s:%s";
	private static final String messageAddressLock = "crm:message:lock:fromuser:%s:%s:touser:%s:%s";
	private static final String JSONStringWithDateFormat = "yyyy-MM-dd HH:mm:ss";
	public static final int onlineUserInfoSaveSecond = 10;
	private static final int justOnlineUserInfoSaveSecond = 12;
	private static final int messageAddressLockTime = 200;
	private static final int messageAddressLockWaitTime = 2000;
	private static final Logger log = LoggerFactory.getLogger(ChatWebSocketServer.class);
	
	private static ChatMessageProviderService mqProviderService;
	private static RedisUtil redisUtil;
	private static RedisLock redisLock;
	
	private ChatMessageProviderService getMQProviderService() {
		if (mqProviderService == null) {
			mqProviderService = ApplicationContextHolder.getBean("mqProviderServiceForNonPersistent");
		}
		return mqProviderService;
	}

	public static RedisLock redisLock() {
		if (redisLock == null) {
			redisLock = ApplicationContextHolder.getBean("redisLock");
		}
		return redisLock;
	}

	public static RedisUtil getRedisUtil() {
		if (redisUtil == null) {
			redisUtil = ApplicationContextHolder.getBean("redisUtil");
		}
		return redisUtil;
	}
	
	//拉取信息
	public void pullMessage(ChatDto<ChatMessage,ChatMessage> dto,ChatUser toUser) throws IOException {
		ChatRequest<ChatMessage> request = dto.getRequest();
		ChatMessage message = new ChatMessage();
		request.setData(message);
		message.setToUsers(Arrays.asList(toUser));
		pullMessageCore(dto,toUser);
		pullUnreadMessagesCore(request.getFromUser(),toUser);
	}
	
	//发送信息
	public void sendMessage(ChatDto<ChatMessage,ChatMessage> dto,ChatUser fromUset) {
		ChatRequest<ChatMessage> request = dto.getRequest();
		ChatResponse<ChatMessage> response = dto.getResponse();
		response.setMessageType(ChatMessageTypeEnum.contentText.toString());
		ChatMessage message = request.getData();
		ChatMessage resData = new ChatMessage();
		resData.setAction(message.getAction());
		resData.setContentText(message.getContentText());
		resData.setProduceDate(message.getProduceDate());
		resData.setToUsers(message.getToUsers());
		// 追加发送人(防止串改)
		request.setFromUser(fromUset);
		message.setProduceDate(new Date());
		log.info("用户消息:" + fromUset.toString() + ",报文:" + message.toString());
		// 可以群发消息
		// 消息保存到数据库redis
		try {
			// 存入redis
			if(!(message.getToUsers()==null||message.getToUsers().size()==0)) {
				for(ChatUser toUser:message.getToUsers()) {
					if(!(toUser.getType()==null||"".equals(toUser.getType())
							||toUser.getId()==null||"".equals(toUser.getId()))) {
						response.setToUser(toUser);
						getRedisUtil().lSet(String.format(messageAddress,request.getFromUser().getType(),request.getFromUser().getId(),toUser.getType(),toUser.getId()),
								JSONObject.toJSONStringWithDateFormat(dto, JSONStringWithDateFormat,SerializerFeature.WriteDateUseDateFormat));
						// 发送消息
						getMQProviderService().send(JSONObject.toJSONStringWithDateFormat(dto, JSONStringWithDateFormat,SerializerFeature.WriteDateUseDateFormat));
					}else{
						getRedisUtil().lSet(String.format(messageAddress,request.getFromUser().getType(),request.getFromUser().getId(),null,null),
								JSONObject.toJSONStringWithDateFormat(dto, JSONStringWithDateFormat,SerializerFeature.WriteDateUseDateFormat));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void pullUserMessage(ChatDto<ChatMessage,ChatMessage> dto,ConcurrentHashMap<String, ChatWebSocketServer> webSocketMap) throws IOException {
		String userTypeAndId = String.format("%s:%s", dto.getResponse().getToUser().getType(), dto.getResponse().getToUser().getId());
		if (StringUtils.isNotBlank(userTypeAndId) && webSocketMap.containsKey(userTypeAndId)) {
			String uuid = UUID.randomUUID().toString();
			boolean bl = redisLock().lock(String.format(messageAddressLock,dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),dto.getResponse().getToUser().getType(), dto.getResponse().getToUser().getId()), uuid,
					messageAddressLockTime, messageAddressLockWaitTime);
			if (bl) {
				List<Object> dtoJsonStrs = getRedisUtil().lGet(String.format(messageAddress,dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),dto.getResponse().getToUser().getType(), dto.getResponse().getToUser().getId()), 0, -1);
				getRedisUtil().del(String.format(messageAddress,dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),dto.getResponse().getToUser().getType(), dto.getResponse().getToUser().getId()));
				redisLock().unlock(String.format(messageAddressLock,dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),dto.getResponse().getToUser().getType(), dto.getResponse().getToUser().getId()), uuid);
				if(!(dtoJsonStrs==null||dtoJsonStrs.size()==0)) {
					for (Object dtoJsonStr : dtoJsonStrs) {
						// 此处需要保存到数据库
						JSONObject dtoJson = JSONObject.parseObject(dtoJsonStr.toString());
						ChatDto<ChatMessage,ChatMessage> dtoTemp = (ChatDto<ChatMessage,ChatMessage>)JSONObject.parseObject(dtoJsonStr.toString(), new TypeReference<ChatDto<ChatMessage,ChatMessage>>(){});
						ChatRequest<ChatMessage> requestTemp = dtoTemp.getRequest();
						dto.getResponse().setData(Arrays.asList(requestTemp.getData()));
					}
					sendInfo(dto);
				}
			}
		} else {
			log.error("用户" + userTypeAndId + ",不在线！");
			subOnlineCount(dto.getResponse().getToUser());
		}
	}
	
	public static void pullUnreadMessagesCore(ChatUser fromUser,ChatUser toUser) throws IOException {
		String fromUserType = "*";
		String fromUserId = "*";
		Set<Object> keys = new HashSet<Object>();
		Set<Object> keysft = null;
		Set<Object> keyst = null;
		Set<Object> keysf = null;
		
		if(!(fromUser==null||fromUser.getType()==null||"".equals(fromUser.getType())
				||fromUser.getId()==null||"".equals(fromUser.getId())
				||toUser==null||toUser.getType()==null||"".equals(toUser.getType())
				||toUser.getId()==null||"".equals(toUser.getId()))) {
			//指定对话信息
			keysft = getRedisUtil().keys(String.format(messageAddress, fromUser.getType(),fromUser.getId(),toUser.getType(),toUser.getId()));
			keys.addAll(keysft);
		}else {
			//未指定接收人的全部信息
			keysf = getRedisUtil().keys(String.format(messageAddress, "*","*",null,null));
			keys.addAll(keysf);
			if(!(toUser==null||toUser.getType()==null||"".equals(toUser.getType())
					||toUser.getId()==null||"".equals(toUser.getId()))) {
				//指定接收人的全部信息
				keyst = getRedisUtil().keys(String.format(messageAddress, "*","*",toUser.getType(),toUser.getId()));
				keys.addAll(keyst);
			}
		}
		//初始未读消息DTO
		ChatDto<ChatMessage,ChatOnline> dto = new ChatDto<ChatMessage,ChatOnline>();
		ChatOnline online = new ChatOnline();
		online.setUnreadCustomerUsers(new ArrayList<ChatUser>());
		online.setUnreadServiceUsers(new ArrayList<ChatUser>());
		
		Map<String,ChatUser> userMap = new HashMap<String,ChatUser>();
		for (Object key : keys) {
			long messageSize = getRedisUtil().lGetListSize(key.toString());
			String dtoJsonStr = (String)getRedisUtil().lGetIndex(key.toString(),0);
			dto = (ChatDto<ChatMessage,ChatOnline>)JSONObject.parseObject(dtoJsonStr, new TypeReference<ChatDto<ChatMessage,ChatOnline>>(){});
			String userTypeAndId = String.format("%s:%s", dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId());
			//按信息发送者，累加未读消息
			ChatUser chatUser = null;
			if(userMap.get(userTypeAndId)==null) {
				chatUser = new ChatUser(dto.getRequest().getFromUser().getId(),dto.getRequest().getFromUser().getType(),messageSize);
				userMap.put(userTypeAndId, chatUser);
			}else {
				chatUser = userMap.get(userTypeAndId);
				chatUser.setToMeUnreadMessageCount(chatUser.getToMeUnreadMessageCount()+messageSize);
			}
		}
		
		//分别加在不同的用户组里
		for (Entry<String,ChatUser> entry : userMap.entrySet()) {
			if(entry.getValue().getType().equals(ChatUserTypeEnum.service.toString())) {
				online.getUnreadServiceUsers().add(entry.getValue());
			}else if(entry.getValue().getType().equals(ChatUserTypeEnum.customer.toString())) {
				online.getUnreadCustomerUsers().add(entry.getValue());
			}
		}
		
		dto.getResponse().setMessageType(ChatMessageTypeEnum.unreadMessage.toString());
		dto.getResponse().setData(Arrays.asList(online));
		if(!(toUser==null||toUser.getType()==null||"".equals(toUser.getType())
				||toUser.getId()==null||"".equals(toUser.getId()))) {
			//指定接收者
			dto.getResponse().setToUser(toUser);
			sendInfo(dto);
		}else if(!(keysf==null||keysf.size()==0)){
			List<Object> serviceUsers = getRedisUtil().getList(String.format(onlineUser, ChatUserTypeEnum.service.toString(), "*"));
			//发送给在线的客服人员
			for(Object user:serviceUsers) {
				ChatUser chatUser = JSONObject.parseObject(user.toString(), ChatUser.class);
				dto.getResponse().setToUser(new ChatUser(chatUser.getId(), chatUser.getType()));
				sendInfo(dto);
			}
		}
	}
	
	public void pullMessageCore(ChatDto<ChatMessage,ChatMessage> dto,ChatUser toUser) throws IOException {
		String uuid = UUID.randomUUID().toString();
		List<Object> messages = new ArrayList<Object>();
		if(toUser.getType().equals(ChatUserTypeEnum.service.toString())) {
			boolean bl = redisLock().lock(String.format(messageAddressLock, dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),null,null), uuid,
					messageAddressLockTime, messageAddressLockWaitTime);
			if(bl) {
				List<Object> messagesNotToUser = getRedisUtil().lGet(String.format(messageAddress, dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),null,null), 0, -1);
				messages.addAll(messagesNotToUser);
				getRedisUtil().del(String.format(messageAddress, dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),null,null));
				redisLock().unlock(String.format(messageAddressLock, dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),null,null), uuid);
			}
		}
		boolean blMy = redisLock().lock(String.format(messageAddressLock, dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),toUser.getType(),toUser.getId()), uuid,
				messageAddressLockTime, messageAddressLockWaitTime);
		if(blMy) {
			List<Object> messagesMy = getRedisUtil().lGet(String.format(messageAddress, dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),toUser.getType(),toUser.getId()), 0, -1);
			getRedisUtil().del(String.format(messageAddress, dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),toUser.getType(),toUser.getId()));
			redisLock().unlock(String.format(messageAddressLock, dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),toUser.getType(),toUser.getId()), uuid);
			messages.addAll(messagesMy);
		}
		dto.getResponse().setData(new ArrayList<ChatMessage>());
//		List<ChatDto<ChatMessage,ChatMessage>> dtoTempList = new ArrayList<ChatDto<ChatMessage,ChatMessage>>();
		if(!(messages==null||messages.size()==0)) {
			for (Object dtoJsonStr : messages) {
				// 此处需要保存到数据库
				ChatDto<ChatMessage,ChatMessage> dtoTemp = (ChatDto<ChatMessage,ChatMessage>)JSONObject.parseObject(dtoJsonStr.toString(), new TypeReference<ChatDto<ChatMessage,ChatMessage>>(){});
				dtoTemp.getRequest().getData().setToUsers(dto.getRequest().getData().getToUsers());
	//			dtoTempList.add(dtoTemp);
				dto.getResponse().getData().add(dtoTemp.getRequest().getData());
			}
			//调试后可以删除
			if(!(dto.getResponse().getData()==null||dto.getResponse().getData().size()==0)) {
				if(!(dto.getRequest().getData().getToUsers()==null||dto.getRequest().getData().getToUsers().size()==0)) {
					for(ChatUser toTempUser:dto.getRequest().getData().getToUsers()) {
						dto.getResponse().setToUser(toTempUser);
	//						getRedisUtil().lSet(String.format(messageAddress,dto.getRequest().getFromUser().getType(),dto.getRequest().getFromUser().getId(),dto.getResponse().getToUser().getType(),dto.getResponse().getToUser().getId()), dtoTempList);
						sendInfo(dto);
					}
				}
			}
		}
	}
	
	public void loadChatOnlineUsersCore(List<ChatUser> userList,boolean isJust) throws IOException {
		if(userList==null) {
			userList = new ArrayList<ChatUser>();
			List<Object> userListJsonStr = getRedisUtil().getList(String.format(onlineUser, "*", "*"));
			for(Object userJsonStr:userListJsonStr) {
				ChatUser user = JSONObject.parseObject(userJsonStr.toString(), ChatUser.class);
				userList.add(user);
			}
		}
		ChatDto<Object, Object> dto = new ChatDto<Object, Object>();
		ChatOnline chatOnlineUsers = new ChatOnline();
		chatOnlineUsers.setJust(isJust);
		dto.getResponse().setMessageType(ChatMessageTypeEnum.onlineInfo.toString());
		chatOnlineUsers.setCustomerCount(getOnlineCount(ChatUserTypeEnum.customer.toString()));
		chatOnlineUsers.setServiceCount(getOnlineCount(ChatUserTypeEnum.service.toString()));
		List<Object> serviceUsers = null;
		List<Object> customerUsers = null;
		List<Object> offlineServiceUsers = null;
		List<Object> offlineCustomerUsers = null;
		if(chatOnlineUsers.isJust()) {
			serviceUsers = getRedisUtil().getList(String.format(justOnlineUser, ChatUserTypeEnum.service.toString(), "*"));
			customerUsers = getRedisUtil().getList(String.format(justOnlineUser, ChatUserTypeEnum.customer.toString(), "*"));
			
			offlineServiceUsers = getRedisUtil().getList(String.format(justOfflineUser, ChatUserTypeEnum.service.toString(), "*"));
			offlineCustomerUsers = getRedisUtil().getList(String.format(justOfflineUser, ChatUserTypeEnum.customer.toString(), "*"));
		}else {
			serviceUsers = getRedisUtil().getList(String.format(onlineUser, ChatUserTypeEnum.service.toString(), "*"));
			customerUsers = getRedisUtil().getList(String.format(onlineUser, ChatUserTypeEnum.customer.toString(), "*"));
		}
		chatOnlineUsers.setCustomerUsers(new ArrayList<ChatUser>());
		chatOnlineUsers.setServiceUsers(new ArrayList<ChatUser>());
		
		//在线客服
		for(Object user:serviceUsers) {
			ChatUser chatUser = JSONObject.parseObject(user.toString(), ChatUser.class);
			chatOnlineUsers.getServiceUsers().add(chatUser);
		}
		//在线客户
		for(Object user:customerUsers) {
			ChatUser chatUser = JSONObject.parseObject(user.toString(), ChatUser.class);
			chatOnlineUsers.getCustomerUsers().add(chatUser);
		}
		
		if(chatOnlineUsers.isJust()) {
			chatOnlineUsers.setOfflineCustomerUsers(new ArrayList<ChatUser>());
			chatOnlineUsers.setOfflineServiceUsers(new ArrayList<ChatUser>());
			//在线客服
			for(Object user:offlineServiceUsers) {
				ChatUser chatUser = JSONObject.parseObject(user.toString(), ChatUser.class);
				chatOnlineUsers.getOfflineServiceUsers().add(chatUser);
			}
			//在线客户
			for(Object user:offlineCustomerUsers) {
				ChatUser chatUser = JSONObject.parseObject(user.toString(), ChatUser.class);
				chatOnlineUsers.getOfflineCustomerUsers().add(chatUser);
			}
		}
		
		long tempCustomerCount = chatOnlineUsers.getCustomerCount();
		List<ChatUser> tempCustomerUsers = chatOnlineUsers.getCustomerUsers();
		List<ChatUser> tempOfflineCustomerUsers = chatOnlineUsers.getOfflineCustomerUsers();
		
		//告知给哪些用户
		for(ChatUser user:userList) {
			dto.getResponse().setToUser(new ChatUser(user.getId(), user.getType()));
			//如果通知的用户是客户，则把在线客户隐藏；如是客服，则不隐藏
			if(user.getType().equals(ChatUserTypeEnum.customer.toString())) {
				chatOnlineUsers.setCustomerCount(0);
				chatOnlineUsers.setCustomerUsers(null);
				chatOnlineUsers.setOfflineCustomerUsers(null);
			}else {
				chatOnlineUsers.setCustomerCount(tempCustomerCount);
				chatOnlineUsers.setCustomerUsers(tempCustomerUsers);
				chatOnlineUsers.setOfflineCustomerUsers(tempOfflineCustomerUsers);
			}
			dto.getResponse().setData(Arrays.asList(chatOnlineUsers));
			sendInfo(dto);
		}
	}
	
	/**
	 * 发送自定义消息
	 */
	public static <P,R> void sendInfo(ChatDto<P,R> dto) throws IOException {
		String userTypeAndId = String.format("%s:%s", dto.getResponse().getToUser().getType(),dto.getResponse().getToUser().getId());
		if (StringUtils.isNotBlank(userTypeAndId) && ChatWebSocketServer.webSocketMap.containsKey(userTypeAndId)) {
//			log.info("发送消息报文:" + JSONObject.toJSONStringWithDateFormat(dto, JSONStringWithDateFormat,SerializerFeature.WriteDateUseDateFormat));
			ChatWebSocketServer.webSocketMap.get(userTypeAndId).sendMessageNow(JSONObject.toJSONStringWithDateFormat(dto, JSONStringWithDateFormat,SerializerFeature.WriteDateUseDateFormat));
		} else {
			log.error("用户" + userTypeAndId + ",不在线！");
		}
	}
	
	public long getOnlineCount(String userType) {
		long count = getRedisUtil().getSize(String.format(onlineUser, userType, "*"));
		return count;
	}

	public void addOnlineCount(ChatUser chatUser) {
		getRedisUtil().set(String.format(onlineUser,chatUser.getType(),chatUser.getId()), JSON.toJSONString(chatUser), onlineUserInfoSaveSecond);
		getRedisUtil().del(String.format(justOfflineUser, chatUser.getType(),chatUser.getId()));
		getRedisUtil().set(String.format(justOnlineUser, chatUser.getType(),chatUser.getId()), JSON.toJSONString(chatUser),justOnlineUserInfoSaveSecond);
	}

	public static void subOnlineCount(ChatUser chatUser) {
		Object obj = getRedisUtil().get(String.format(onlineUser, chatUser.getType(),chatUser.getId()));
		getRedisUtil().del(String.format(onlineUser, chatUser.getType(),chatUser.getId()));
		getRedisUtil().del(String.format(justOnlineUser, chatUser.getType(),chatUser.getId()));
		getRedisUtil().set(String.format(justOfflineUser, chatUser.getType(),chatUser.getId()),obj,justOnlineUserInfoSaveSecond);
	}
	
	
}
