package com.box.chat.pojo;

import java.io.Serializable;
import java.util.Date;

public class ChatDto<T> implements Serializable {
	private static final long serialVersionUID = 1695176968770791034L;

	private String action;
	private String messageType = ChatMessageTypeEnum.contentText.toString();
	private ChatUser targetUser;
	private T data;

	public ChatDto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public ChatUser getTargetUser() {
		return targetUser;
	}

	public void setTargetUser(ChatUser targetUser) {
		this.targetUser = targetUser;
	}
	
	public static void main(String[] args) {
		System.out.println(new Date(System.currentTimeMillis()));
	}
}
