package com.box.chat.service;

import java.io.IOException;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.box.chat.pojo.ChatDto;
import com.box.chat.pojo.ChatMessage;

@Service
public class ChatMessageReceiverService implements JmsListenerConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(ChatMessageReceiverService.class);
	
	@Value("${spring.activemq.quere-online-notice}")
	private String destinaionName;
	
	@Value("${com.box.chat.node}")
	private String chatNode;
	
	@Autowired
	private ChatWebSocketService chatWebSocketService;
	
    @Override
    public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
        SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
        endpoint.setId(chatNode);
        endpoint.setDestination(destinaionName+":"+chatNode);
        
        endpoint.setMessageListener(message -> {
            TextMessage tm = (TextMessage) message;
            try {
            	String text = tm.getText();
            	if(!(text==null||"".equals(text))) {
            		ChatDto<ChatMessage> dto = (ChatDto<ChatMessage>)JSONObject.parseObject(text, new TypeReference<ChatDto<ChatMessage>>(){});
            		chatWebSocketService.noticeUnreadMessagesCore(dto.getData().getFromUser(),dto.getData().getToUser());
            	}
            	log.info("endpoint.setMessageListener--->" + text);
            } catch (JMSException e) {
                e.printStackTrace();
            } catch (IOException e) {
				e.printStackTrace();
			}
        });
        registrar.registerEndpoint(endpoint);
    }
}