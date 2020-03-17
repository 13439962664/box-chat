package com.box.chat.service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Queue;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.box.chat.controller.ChatWebSocketServer;
import com.box.chat.dao.ChatMessageDao;
import com.box.chat.pojo.ChatDto;
import com.box.chat.pojo.ChatMessage;
import com.box.chat.pojo.ChatMessageTypeEnum;
import com.box.chat.pojo.ChatOnline;
import com.box.chat.pojo.ChatUser;
import com.box.chat.pojo.ChatUserTypeEnum;
import com.box.utils.RedisUtil;
import com.box.utils.lock.RedisLock;

@Component
public class ChatWebSocketService {

	public static final String onlineUser = "crm:online:online:%s:%s";
	private static final String justOnlineUser = "crm:online:justonline:%s:%s";
	private static final String justOfflineUser = "crm:online:justoffline:%s:%s";
	private static final String messageAddress = "crm:message:fromuser:%s:%s:touser:%s:%s";
	private static final String messageAddressLock = "crm:message:lock:fromuser:%s:%s:touser:%s:%s";
	private static final String JSONStringWithDateFormat = "yyyy-MM-dd HH:mm:ss:SSS";
	public static final int onlineUserInfoSaveSecond = 10;
	private static final int justOnlineUserInfoSaveSecond = 12;
	private static final int messageAddressLockTime = 200;
	private static final int messageAddressLockWaitTime = 2000;
	private static final int sendMessageHisEveryReadLimit = 5;
	private static final Logger log = LoggerFactory.getLogger(ChatWebSocketServer.class);
	
	@Value("${spring.activemq.quere-online-notice}") String quereName;
	@Value("${com.box.chat.node}")
	private String chatNode;
	
	@Autowired
	private ChatMessageProviderService chatMessageNonPersistentProviderService;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private RedisLock redisLock;
	
	@Autowired
	private ChatMessageDao chatMessageDao;
	
	//拉取已读历史信息
	public void sendMessageHis(ChatDto<ChatMessage> dto,ChatUser myUser) throws IOException, ParseException {
		ChatDto<List<ChatMessage>> resultDto = new ChatDto<List<ChatMessage>>();
		List<ChatMessage> listCM = sendMessageHisUnread(dto,myUser); 
		int everyReadLimit = sendMessageHisEveryReadLimit;
		if(!(listCM==null||listCM.size()==0)) {
			everyReadLimit = everyReadLimit-listCM.size();
		}
		if(everyReadLimit>0) {
			Date lastDate = dto.getTargetDate()==null||"".equals(dto.getTargetDate())?null:new SimpleDateFormat(JSONStringWithDateFormat).parse(dto.getTargetDate()); 
			List<ChatDto> listDto = chatMessageDao.findMessageHis(myUser,dto.getTargetUser(),lastDate,everyReadLimit);
			if(!(listDto==null||listDto.size()==0)) {
				for(ChatDto<ChatMessage> dtoTemp:listDto) {
//					dtoTemp.setMessageType(ChatMessageTypeEnum.historyContentText.toString());
//					dtoTemp.setTargetUser(myUser);
					listCM.add(dtoTemp.getData());
//					sendInfo(dtoTemp);
				}
			}
		}
		
		if(!(listCM==null||listCM.size()==0)) {
			resultDto.setData(listCM);
			resultDto.setTargetUser(myUser);
			resultDto.setTargetDate(dto.getTargetDate());
			resultDto.setMessageType(ChatMessageTypeEnum.historyContentText.toString());
			sendInfo(resultDto);
		}
	}
	
	//拉取对方未读历史信息
	public List<ChatMessage> sendMessageHisUnread(ChatDto<ChatMessage> dto,ChatUser myUser) throws IOException {
//		ChatDto<List<ChatMessage>> resultDto = new ChatDto<List<ChatMessage>>();
		List<ChatMessage> listCM = new ArrayList<ChatMessage>(); 
		List<Object> dtoJsonStrs = redisUtil.lGet(String.format(messageAddress,myUser.getType(),myUser.getId(),dto.getTargetUser().getType(), dto.getTargetUser().getId()), 0, -1);
		if(!(dtoJsonStrs==null||dtoJsonStrs.size()==0)) {
			for (int i=0;i<dtoJsonStrs.size();i++) {
				String dtoJsonStr = (String)dtoJsonStrs.get(dtoJsonStrs.size()-i-1);
				ChatDto<ChatMessage> dtoTemp = (ChatDto<ChatMessage>)JSONObject.parseObject(dtoJsonStr.toString(), new TypeReference<ChatDto<ChatMessage>>(){});
				listCM.add(dtoTemp.getData());
//				sendInfo(dtoTemp);
			}
//			resultDto.setTargetUser(myUser);
//			resultDto.setMessageType(ChatMessageTypeEnum.historyUnreadContentText.toString());
//			resultDto.setData(listCM);
//			sendInfo(resultDto);
		}
		return listCM;
	}
	
	//拉取信息
	public void pullMessage(ChatDto<ChatMessage> dto,ChatUser myUser) throws IOException {
		ChatMessage message = new ChatMessage();
		dto.setData(message);
		message.setToUser(myUser);
		pullMessageCore(dto,myUser);
//		pullUnreadMessagesCore(message.getFromUser(),myUser);
	}
	
	//发送信息
	public void sendMessage(ChatDto<ChatMessage> dto,ChatUser myUser) {
		dto.setMessageType(ChatMessageTypeEnum.contentText.toString());
		ChatMessage message = dto.getData();
		message.setToUser(dto.getTargetUser());
		// 追加发送人(防止串改)
		message.setFromUser(myUser);
		message.setProduceDate(Calendar.getInstance().getTime());
		log.info("用户消息:" + myUser.toString() + ",报文:" + message.toString());
		// 可以群发消息
		// 消息保存到数据库redis
		try {
			// 存入redis
			if(!(message.getToUser().getType()==null||"".equals(message.getToUser().getType())
					||message.getToUser().getId()==null||"".equals(message.getToUser().getId()))) {
				dto.setTargetUser(message.getToUser());
				redisUtil.lSet(String.format(messageAddress,dto.getData().getFromUser().getType(),dto.getData().getFromUser().getId(),message.getToUser().getType(),message.getToUser().getId()),
						JSONObject.toJSONString(dto, SerializerFeature.UseISO8601DateFormat));
				Object user = redisUtil.get(String.format(onlineUser, message.getToUser().getType(),message.getToUser().getId()));
				if(user!=null) {
					ChatUser chatUser = JSONObject.parseObject(user.toString(), ChatUser.class);
					Queue queue = new ActiveMQQueue(quereName+":"+chatUser.getOnlineNode());
					// 发送消息
					chatMessageNonPersistentProviderService.send(queue,JSONObject.toJSONString(dto, SerializerFeature.UseISO8601DateFormat));
				}
			}else{
				redisUtil.lSet(String.format(messageAddress,dto.getData().getFromUser().getType(),dto.getData().getFromUser().getId(),null,null),
						JSONObject.toJSONString(dto, SerializerFeature.UseISO8601DateFormat));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Transactional
	public void pullUserMessage(ChatDto<ChatMessage> dto,ConcurrentHashMap<String, ChatWebSocketServer> webSocketMap) throws IOException {
		String userTypeAndId = String.format("%s:%s", dto.getTargetUser().getType(), dto.getTargetUser().getId());
		if (StringUtils.isNotBlank(userTypeAndId) && webSocketMap.containsKey(userTypeAndId)) {
			String uuid = UUID.randomUUID().toString();
			boolean bl = redisLock.lock(String.format(messageAddressLock,dto.getData().getFromUser().getType(),dto.getData().getFromUser().getId(),dto.getTargetUser().getType(), dto.getTargetUser().getId()), uuid,
					messageAddressLockTime, messageAddressLockWaitTime);
			if (bl) {
				List<Object> dtoJsonStrs = redisUtil.lGet(String.format(messageAddress,dto.getData().getFromUser().getType(),dto.getData().getFromUser().getId(),dto.getTargetUser().getType(), dto.getTargetUser().getId()), 0, -1);
				
				if(!(dtoJsonStrs==null||dtoJsonStrs.size()==0)) {
					for (Object dtoJsonStr : dtoJsonStrs) {
						// 此处需要保存到数据库
						JSONObject dtoJson = JSONObject.parseObject(dtoJsonStr.toString());
						ChatDto<ChatMessage> dtoTemp = (ChatDto<ChatMessage>)JSONObject.parseObject(dtoJsonStr.toString(), new TypeReference<ChatDto<ChatMessage>>(){});
						dto.setData(dtoTemp.getData());
						dto.getData().setToUser(dto.getTargetUser());
						dto.getData().setReadDate(new Date(System.currentTimeMillis()));
						sendInfo(dto);
						chatMessageDao.saveMessage(dto);
					}
				}
				redisUtil.del(String.format(messageAddress,dto.getData().getFromUser().getType(),dto.getData().getFromUser().getId(),dto.getTargetUser().getType(), dto.getTargetUser().getId()));
				redisLock.unlock(String.format(messageAddressLock,dto.getData().getFromUser().getType(),dto.getData().getFromUser().getId(),dto.getTargetUser().getType(), dto.getTargetUser().getId()), uuid);
			}
		} else {
			log.error("用户" + userTypeAndId + ",不在线！");
			subOnlineCount(dto.getTargetUser());
		}
	}
	
	public void noticeUnreadMessagesCore(ChatUser fromUser,ChatUser toUser) throws IOException {
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
			keysft = redisUtil.keys(String.format(messageAddress, fromUser.getType(),fromUser.getId(),toUser.getType(),toUser.getId()));
			keys.addAll(keysft);
		}else {
			//未指定接收人的全部信息
			keysf = redisUtil.keys(String.format(messageAddress, "*","*",null,null));
			keys.addAll(keysf);
			if(!(toUser==null||toUser.getType()==null||"".equals(toUser.getType())
					||toUser.getId()==null||"".equals(toUser.getId()))) {
				//指定接收人的全部信息
				keyst = redisUtil.keys(String.format(messageAddress, "*","*",toUser.getType(),toUser.getId()));
				keys.addAll(keyst);
			}
		}
		//初始未读消息DTO
		ChatDto<ChatMessage> dtoM = new ChatDto<ChatMessage>();
		ChatDto<ChatOnline> dto = new ChatDto<ChatOnline>();
		ChatOnline online = new ChatOnline();
		online.setUnreadCustomerUsers(new ArrayList<ChatUser>());
		online.setUnreadServiceUsers(new ArrayList<ChatUser>());
		
		Map<String,ChatUser> userMap = new HashMap<String,ChatUser>();
		for (Object key : keys) {
			long messageSize = redisUtil.lGetListSize(key.toString());
			String dtoJsonStr = (String)redisUtil.lGetIndex(key.toString(),0);
			dtoM = (ChatDto<ChatMessage>)JSONObject.parseObject(dtoJsonStr, new TypeReference<ChatDto<ChatMessage>>(){});
			String userTypeAndId = String.format("%s:%s", dtoM.getData().getFromUser().getType(),dtoM.getData().getFromUser().getId());
			//按信息发送者，累加未读消息
			ChatUser chatUser = null;
			if(userMap.get(userTypeAndId)==null) {
				chatUser = new ChatUser(dtoM.getData().getFromUser().getId(),dtoM.getData().getFromUser().getType(),messageSize);
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
		dto.setData(online);
		dto.setMessageType(ChatMessageTypeEnum.unreadMessage.toString());
		if(!(toUser==null||toUser.getType()==null||"".equals(toUser.getType())
				||toUser.getId()==null||"".equals(toUser.getId()))) {
			//指定接收者
			dto.setTargetUser(toUser);
			sendInfo(dto);
		}else if(!(keysf==null||keysf.size()==0)){
			List<Object> serviceUsers = redisUtil.getList(String.format(onlineUser, ChatUserTypeEnum.service.toString(), "*"));
			//发送给在线的客服人员
			for(Object user:serviceUsers) {
				ChatUser chatUser = JSONObject.parseObject(user.toString(), ChatUser.class);
				dto.setTargetUser(new ChatUser(chatUser.getId(), chatUser.getType()));
				sendInfo(dto);
			}
		}
	}
	
	@Transactional
	public void pullMessageCore(ChatDto<ChatMessage> dto,ChatUser myUser) throws IOException {
		ChatUser targetUser = dto.getTargetUser();
		String uuid = UUID.randomUUID().toString();
		List<Object> messages = new ArrayList<Object>();
		List<Object> messagesNotToUser = null;
		List<Object> messagesMy = null;
		boolean blNotToUser = false; 
		boolean blMy = false;
		if(myUser.getType().equals(ChatUserTypeEnum.service.toString())) {
			blNotToUser = redisLock.lock(String.format(messageAddressLock, targetUser.getType(),targetUser.getId(),null,null), uuid,
					messageAddressLockTime, messageAddressLockWaitTime);
			if(blNotToUser) {
				messagesNotToUser = redisUtil.lGet(String.format(messageAddress, targetUser.getType(),targetUser.getId(),null,null), 0, -1);
				messages.addAll(messagesNotToUser);
			}
		}
		blMy = redisLock.lock(String.format(messageAddressLock, targetUser.getType(),targetUser.getId(),myUser.getType(),myUser.getId()), uuid,
				messageAddressLockTime, messageAddressLockWaitTime);
		if(blMy) {
			messagesMy = redisUtil.lGet(String.format(messageAddress, targetUser.getType(),targetUser.getId(),myUser.getType(),myUser.getId()), 0, -1);
			messages.addAll(messagesMy);
		}
		
//		List<ChatDto<ChatMessage,ChatMessage>> dtoTempList = new ArrayList<ChatDto<ChatMessage,ChatMessage>>();
		if(!(messages==null||messages.size()==0)) {
			for (Object dtoJsonStr : messages) {
				// 此处需要保存到数据库
				ChatDto<ChatMessage> dtoTemp = (ChatDto<ChatMessage>)JSONObject.parseObject(dtoJsonStr.toString(), new TypeReference<ChatDto<ChatMessage>>(){});
				dtoTemp.getData().setToUser(myUser);
	//			dtoTempList.add(dtoTemp);
				dto.setData(dtoTemp.getData());
				dto.setTargetUser(dto.getData().getToUser());
				dto.getData().setReadDate(new Date(System.currentTimeMillis()));
				sendInfo(dto);
				chatMessageDao.saveMessage(dto);
			}
		}
		if(blNotToUser) {
			if(!(messagesNotToUser==null||messagesNotToUser.size()==0)) {
				redisUtil.del(String.format(messageAddress, targetUser.getType(),targetUser.getId(),null,null));
			}
			redisLock.unlock(String.format(messageAddressLock, targetUser.getType(),targetUser.getId(),null,null), uuid);
		}
		if(blMy) {
			if(!(messagesMy==null||messagesMy.size()==0)) {
				redisUtil.del(String.format(messageAddress, targetUser.getType(),targetUser.getId(),myUser.getType(),myUser.getId()));
			}
			redisLock.unlock(String.format(messageAddressLock, targetUser.getType(),targetUser.getId(),myUser.getType(),myUser.getId()), uuid);
		}
	}
	
	public void loadChatOnlineUsersCore(List<ChatUser> userList,Boolean isJust) throws IOException {
		if(userList==null) {
			userList = new ArrayList<ChatUser>();
			List<Object> userListJsonStr = redisUtil.getList(String.format(onlineUser, "*", "*"));
			for(Object userJsonStr:userListJsonStr) {
				ChatUser user = JSONObject.parseObject(userJsonStr.toString(), ChatUser.class);
				userList.add(user);
			}
		}
		ChatDto<ChatOnline> dto = new ChatDto<ChatOnline>();
		ChatOnline chatOnlineUsers = new ChatOnline();
		chatOnlineUsers.setIsJust(isJust);
		dto.setMessageType(ChatMessageTypeEnum.onlineInfo.toString());
		chatOnlineUsers.setCustomerCount(getOnlineCount(ChatUserTypeEnum.customer.toString()));
		chatOnlineUsers.setServiceCount(getOnlineCount(ChatUserTypeEnum.service.toString()));
		List<Object> serviceUsers = null;
		List<Object> customerUsers = null;
		List<Object> offlineServiceUsers = null;
		List<Object> offlineCustomerUsers = null;
		if(chatOnlineUsers.getIsJust()) {
			serviceUsers = redisUtil.getList(String.format(justOnlineUser, ChatUserTypeEnum.service.toString(), "*"));
			customerUsers = redisUtil.getList(String.format(justOnlineUser, ChatUserTypeEnum.customer.toString(), "*"));
			
			offlineServiceUsers = redisUtil.getList(String.format(justOfflineUser, ChatUserTypeEnum.service.toString(), "*"));
			offlineCustomerUsers = redisUtil.getList(String.format(justOfflineUser, ChatUserTypeEnum.customer.toString(), "*"));
		}else {
			serviceUsers = redisUtil.getList(String.format(onlineUser, ChatUserTypeEnum.service.toString(), "*"));
			customerUsers = redisUtil.getList(String.format(onlineUser, ChatUserTypeEnum.customer.toString(), "*"));
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
		
		if(chatOnlineUsers.getIsJust()) {
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
			dto.setTargetUser(new ChatUser(user.getId(), user.getType()));
			//如果通知的用户是客户，则把在线客户隐藏；如是客服，则不隐藏
			if(user.getType().equals(ChatUserTypeEnum.customer.toString())) {
				chatOnlineUsers.setCustomerCount(null);
				chatOnlineUsers.setCustomerUsers(null);
				chatOnlineUsers.setOfflineCustomerUsers(null);
			}else {
				chatOnlineUsers.setCustomerCount(tempCustomerCount);
				chatOnlineUsers.setCustomerUsers(tempCustomerUsers);
				chatOnlineUsers.setOfflineCustomerUsers(tempOfflineCustomerUsers);
			}
			dto.setData(chatOnlineUsers);
			sendInfo(dto);
		}
	}
	
	/**
	 * 发送自定义消息
	 */
	public static <T> void sendInfo(ChatDto<T> dto) throws IOException {
		String userTypeAndId = String.format("%s:%s", dto.getTargetUser().getType(),dto.getTargetUser().getId());
		if (StringUtils.isNotBlank(userTypeAndId) && ChatWebSocketServer.webSocketMap.containsKey(userTypeAndId)) {
//			log.info("发送消息报文:" + JSONObject.toJSONString(dto, SerializerFeature.UseISO8601DateFormat));
			ChatWebSocketServer.webSocketMap.get(userTypeAndId).sendMessageNow(JSONObject.toJSONStringWithDateFormat(dto,JSONStringWithDateFormat, SerializerFeature.WriteDateUseDateFormat));
		} else {
			log.error("用户" + userTypeAndId + ",不在线！");
		}
	}
	
	public long getOnlineCount(String userType) {
		long count = redisUtil.getSize(String.format(onlineUser, userType, "*"));
		return count;
	}

	public void addOnlineCount(ChatUser chatUser) {
		chatUser.setOnlineNode(this.chatNode);
		redisUtil.set(String.format(onlineUser,chatUser.getType(),chatUser.getId()), JSON.toJSONString(chatUser), onlineUserInfoSaveSecond);
		redisUtil.del(String.format(justOfflineUser, chatUser.getType(),chatUser.getId()));
		redisUtil.set(String.format(justOnlineUser, chatUser.getType(),chatUser.getId()), JSON.toJSONString(chatUser),justOnlineUserInfoSaveSecond);
	}

	public void subOnlineCount(ChatUser chatUser) {
		Object obj = redisUtil.get(String.format(onlineUser, chatUser.getType(),chatUser.getId()));
		redisUtil.del(String.format(onlineUser, chatUser.getType(),chatUser.getId()));
		redisUtil.del(String.format(justOnlineUser, chatUser.getType(),chatUser.getId()));
		redisUtil.set(String.format(justOfflineUser, chatUser.getType(),chatUser.getId()),obj,justOnlineUserInfoSaveSecond);
	}
	
	public void offOnlineDelay(String userType,String userId) {
		redisUtil.expire(String.format(ChatWebSocketService.onlineUser, userType, userId), onlineUserInfoSaveSecond);
	}
}
