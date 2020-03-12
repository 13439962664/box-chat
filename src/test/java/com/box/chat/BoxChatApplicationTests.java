package com.box.chat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.alibaba.fastjson.JSONObject;
import com.box.chat.dao.ChatMessageDao;
import com.box.chat.pojo.ChatDto;
import com.box.chat.pojo.ChatUser;

@SpringBootTest
class BoxChatApplicationTests {

	
	private static final Logger log = LoggerFactory.getLogger(BoxChatApplicationTests.class);

	
	@Autowired
	private ChatMessageDao chatMessageDao;
	
	@Test
	void contextLoads() {
	}

}
