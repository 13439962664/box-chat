package com.box.chat.pojo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ChatMessage implements Serializable {
	private static final long serialVersionUID = -6627365431820129056L;
	private String id;
	private String action;
	private String sessionId;
	private String contentText;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")
	private Date produceDate;
	private List<ChatUser> toUsers;

	public List<ChatUser> getToUsers() {
		return toUsers;
	}

	public void setToUsers(List<ChatUser> toUsers) {
		this.toUsers = toUsers;
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
