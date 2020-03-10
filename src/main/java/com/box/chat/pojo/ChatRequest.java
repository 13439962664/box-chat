package com.box.chat.pojo;

import java.io.Serializable;

public class ChatRequest<T> implements Serializable {
	private static final long serialVersionUID = 749861102648309394L;
	private String action;
	private ChatUser fromUser;
	private T data;
	
	public ChatUser getFromUser() {
		return fromUser;
	}

	public void setFromUser(ChatUser fromUser) {
		this.fromUser = fromUser;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

}
