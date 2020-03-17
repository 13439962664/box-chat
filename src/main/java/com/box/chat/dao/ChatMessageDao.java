package com.box.chat.dao;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.box.chat.pojo.ChatDto;
import com.box.chat.pojo.ChatMessage;
import com.box.chat.pojo.ChatUser;

@Component
public class ChatMessageDao {
	
	private static final Logger log = LoggerFactory.getLogger(ChatMessageDao.class);
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	public int saveMessage(ChatDto<ChatMessage> dto) {
		int status=0;
		mongoTemplate.save(dto);
		return status;
	}
	
	public List<ChatDto> findMessageHis(ChatUser myUser,ChatUser otherUser,Date lastDate,int limit) {
		List<ChatDto> listDto = null;
		Criteria crToMy = Criteria.where("data.toUser.type").is(myUser.getType())
				.and("data.toUser._id").is(myUser.getId())
				.and("data.fromUser.type").is(otherUser.getType())
				.and("data.fromUser._id").is(otherUser.getId());
		Criteria crToOther = Criteria.where("data.toUser.type").is(otherUser.getType())
				.and("data.toUser._id").is(otherUser.getId())
				.and("data.fromUser.type").is(myUser.getType())
				.and("data.fromUser._id").is(myUser.getId());
		Criteria cr = new Criteria().orOperator(crToMy,crToOther);
		if(lastDate!=null) {
			cr.and("data.produceDate").lt(lastDate);
		}
		Query query = new Query(cr);
		query.with(Sort.by(Sort.Direction.DESC,"data.produceDate"));
		query.limit(limit);
		listDto = mongoTemplate.find(query,ChatDto.class);
		log.info(JSONObject.toJSONString(listDto,SerializerFeature.UseISO8601DateFormat));
		return listDto;
	}
	
}
