package com.box.chat.config;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;

import com.box.chat.service.ChatMessageProviderService;

@Configuration
public class ActiveMQConfig {
	
	private static final Logger log = LoggerFactory.getLogger(ActiveMQConfig.class);

	@Value("${spring.activemq.broker-url}")
    private String brokerUrl;

    @Value("${spring.activemq.user}")
    private String username;

    @Value("${spring.activemq.password}")
    private String password;

    @Value("${spring.activemq.topic-name-online-service}")
    private String topicNameOnlineService;
	
	public Topic topic(String topicName) {
		return new ActiveMQTopic(topicName);
	}
	
	@Bean
    public ConnectionFactory connectionFactory(){
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(username, password, brokerUrl);
		connectionFactory.setUseAsyncSend(true);
        return connectionFactory;
    }
	
	private JmsTemplate jmsTemplate(int deliveryMode) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory());
        jmsTemplate.setPriority(9);
        jmsTemplate.setSessionTransacted(true);
        jmsTemplate.setDeliveryMode(DeliveryMode.PERSISTENT);
        jmsTemplate.setExplicitQosEnabled(true);
//        log.info("jmsTemplate SessionTransacted--->"+jmsTemplate.isSessionTransacted());
        return jmsTemplate;
    }

	private JmsMessagingTemplate jmsMessageTemplate(int deliveryMode) {
        JmsMessagingTemplate messagingTemplate = new JmsMessagingTemplate(jmsTemplate(deliveryMode));
//        log.info("messagingTemplate jmsTemplate SessionTransacted--->"+messagingTemplate.getJmsTemplate().isSessionTransacted());
        return messagingTemplate;
    }
    
    @Bean
    public ChatMessageProviderService chatMessageProviderService(@Value("${spring.activemq.topic-name-online-service}") String topicName) {
    	return new ChatMessageProviderService(jmsMessageTemplate(DeliveryMode.NON_PERSISTENT),topic(topicName));
    }
    
  //在Queue模式中，对消息的监听需要对containerFactory进行配置
    @Bean("queueListener")
    public JmsListenerContainerFactory<?> queueJmsListenerContainerFactory(ConnectionFactory connectionFactory){
        SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(false);
        factory.setSessionTransacted(true);
        factory.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
        return factory;
    }
    
    //在Topic模式中，对消息的监听需要对containerFactory进行配置
    @Bean("topicListener")
    public JmsListenerContainerFactory<?> topicJmsListenerContainerFactory(ConnectionFactory connectionFactory){
        SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPubSubDomain(true);
        factory.setSessionTransacted(true);
        factory.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
        return factory;
    }
}
