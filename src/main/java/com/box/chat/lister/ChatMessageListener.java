package com.box.chat.lister;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.box.chat.pojo.ChatDto;
import com.box.chat.pojo.ChatMessage;
import com.box.chat.service.ChatWebSocketService;

@Component
public class ChatMessageListener {
	
	private static final Logger log = LoggerFactory.getLogger(ChatMessageListener.class);

	@Autowired
	private ChatWebSocketService chatWebSocketService;
	
	//queue模式的消费者
    @JmsListener(destination="${spring.activemq.topic-name-online-service}", containerFactory="topicListener")
    public void readActiveTopicOnlineService(String message) throws IOException {
    	if(!(message==null||"".equals(message))) {
    		ChatDto<ChatMessage> dto = (ChatDto<ChatMessage>)JSONObject.parseObject(message, new TypeReference<ChatDto<ChatMessage>>(){});
//    		chatWebSocketService.pullUserMessage(dto);
    		chatWebSocketService.pullUnreadMessagesCore(dto.getData().getFromUser(),dto.getData().getToUser());
    	}
    	log.info("readActiveTopic--->" + message);
    }
}
