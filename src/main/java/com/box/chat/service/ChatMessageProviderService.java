package com.box.chat.service;

import java.util.UUID;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQMessageProducer;
import org.apache.activemq.AsyncCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.support.JmsUtils;
import org.springframework.transaction.annotation.Transactional;

public class ChatMessageProviderService {

	private static final Logger log = LoggerFactory.getLogger(ChatMessageProviderService.class);

	// 注入存放消息的队列，用于下列方法一
	private Destination destination;

	// 注入springboot封装的工具类
	private JmsMessagingTemplate jmsMessagingTemplate;
	
	public ChatMessageProviderService(JmsMessagingTemplate jmsMessagingTemplate,Destination destination) {
		super();
		this.destination = destination;
		this.jmsMessagingTemplate = jmsMessagingTemplate;
	}

	@Transactional
	public String send(String message) throws JMSException {
		return this.sendMessage(destination, message);
	}

	// 发送消息，destination是发送到的队列，message是待发送的消息
	private String sendMessage(Destination destination, String message) throws JMSException {
		String msgID = null;
		msgID = jmsMessagingTemplate.getJmsTemplate().execute(new SessionCallback<String>() {
			@Override
			public String doInJms(Session session) throws JMSException {
				ActiveMQMessageProducer activeMOMessageProducer = (ActiveMQMessageProducer) session.createProducer(destination);
				TextMessage tmessage = null;
				tmessage = session.createTextMessage(message);
				tmessage.setJMSMessageID(UUID.randomUUID().toString());
				String msgID = tmessage.getJMSMessageID();
				activeMOMessageProducer.send(tmessage, new AsyncCallback() {
					@Override
					public void onSuccess() {
						log.info("onSuccess--->" + msgID);
					}

					@Override
					public void onException(JMSException e) {
						log.error("onException--->" + msgID);
					}
				});
				log.info("jmsMessagingTemplate.convertAndSend--->" + message);
				JmsUtils.closeMessageProducer(activeMOMessageProducer);
				return msgID;
			}
		});
		log.info("jmsMessagingTemplate.getJmsTemplate().execute... msgID--->" + msgID);
//		jmsMessagingTemplate.convertAndSend(destination, message);
//		log.info("jmsMessagingTemplate.convertAndSend--->" + message);
//		System.out.println(1/0);
		return msgID;
	}
}
