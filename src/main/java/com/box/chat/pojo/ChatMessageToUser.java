package com.box.chat.pojo;

import java.io.Serializable;

public class ChatMessageToUser implements Serializable {
	private static final long serialVersionUID = -3465064762138834859L;
	private String toUserType;
	private String toUserId;

	public String getToUserType() {
		return toUserType;
	}

	public void setToUserType(String toUserType) {
		this.toUserType = toUserType;
	}

	public String getToUserId() {
		return toUserId;
	}

	public void setToUserId(String toUserId) {
		this.toUserId = toUserId;
	}

}
