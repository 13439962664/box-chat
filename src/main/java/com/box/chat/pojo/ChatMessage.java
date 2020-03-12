package com.box.chat.pojo;

import java.io.Serializable;
import java.util.Date;

public class ChatMessage implements Serializable {
	private static final long serialVersionUID = -6627365431820129056L;
	private String id;
	private String action;
	private String sessionId;
	private String contentText;
	private Date produceDate;
	private Date readDate;
	private ChatUser toUser;
	private ChatUser fromUser;

	public Date getReadDate() {
		return readDate;
	}

	public void setReadDate(Date readDate) {
		this.readDate = readDate;
	}

	public ChatUser getFromUser() {
		return fromUser;
	}

	public void setFromUser(ChatUser fromUser) {
		this.fromUser = fromUser;
	}

	public ChatUser getToUser() {
		return toUser;
	}

	public void setToUser(ChatUser toUser) {
		this.toUser = toUser;
	}

	public String getContentText() {
		return contentText;
	}

	public void setContentText(String contentText) {
		this.contentText = contentText;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Date getProduceDate() {
		return produceDate;
	}

	public void setProduceDate(Date produceDate) {
		this.produceDate = produceDate;
	}

}
