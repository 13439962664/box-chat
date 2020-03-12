package com.box.chat.pojo;

import java.io.Serializable;

public class ChatResponse<T> implements Serializable {
	private static final long serialVersionUID = 1695176968770791034L;
	private ChatUser toUser;
	private String messageType = ChatMessageTypeEnum.contentText.toString();
	private T data;

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public ChatUser getToUser() {
		return toUser;
	}

	public void setToUser(ChatUser toUser) {
		this.toUser = toUser;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

}
