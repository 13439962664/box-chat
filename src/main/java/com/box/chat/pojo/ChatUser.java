package com.box.chat.pojo;

import java.io.Serializable;

public class ChatUser implements Serializable {
	private static final long serialVersionUID = -7917604429240552798L;
	private String id;
	private String type = ChatUserTypeEnum.customer.toString();
	private String name;
	private String onlineNode;
	private Long toMeUnreadMessageCount;

	public ChatUser() {
		super();
	}

	public ChatUser(String id, String type) {
		super();
		this.id = id;
		this.type = type;
	}

	public ChatUser(String id, String type, long toMeUnreadMessageCount) {
		super();
		this.id = id;
		this.type = type;
		this.toMeUnreadMessageCount = toMeUnreadMessageCount;
	}

	@Override
	public String toString() {
		return "ChatUser [id=" + id + ", type=" + type + ", name=" + name + "]";
	}

	public String getOnlineNode() {
		return onlineNode;
	}

	public void setOnlineNode(String onlineNode) {
		this.onlineNode = onlineNode;
	}

	public Long getToMeUnreadMessageCount() {
		return toMeUnreadMessageCount;
	}

	public void setToMeUnreadMessageCount(Long toMeUnreadMessageCount) {
		this.toMeUnreadMessageCount = toMeUnreadMessageCount;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
