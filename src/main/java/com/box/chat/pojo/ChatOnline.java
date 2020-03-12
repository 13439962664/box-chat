package com.box.chat.pojo;

import java.io.Serializable;
import java.util.List;

public class ChatOnline implements Serializable {
	private static final long serialVersionUID = 2330638623217900449L;
	private Boolean isJust;
	private Long customerCount;
	private Long serviceCount;
	private List<ChatUser> customerUsers;
	private List<ChatUser> serviceUsers;

	private List<ChatUser> offlineCustomerUsers;
	private List<ChatUser> offlineServiceUsers;

	private List<ChatUser> unreadCustomerUsers;
	private List<ChatUser> unreadServiceUsers;

	public List<ChatUser> getUnreadCustomerUsers() {
		return unreadCustomerUsers;
	}

	public void setUnreadCustomerUsers(List<ChatUser> unreadCustomerUsers) {
		this.unreadCustomerUsers = unreadCustomerUsers;
	}

	public List<ChatUser> getUnreadServiceUsers() {
		return unreadServiceUsers;
	}

	public void setUnreadServiceUsers(List<ChatUser> unreadServiceUsers) {
		this.unreadServiceUsers = unreadServiceUsers;
	}

	public List<ChatUser> getOfflineCustomerUsers() {
		return offlineCustomerUsers;
	}

	public void setOfflineCustomerUsers(List<ChatUser> offlineCustomerUsers) {
		this.offlineCustomerUsers = offlineCustomerUsers;
	}

	public List<ChatUser> getOfflineServiceUsers() {
		return offlineServiceUsers;
	}

	public void setOfflineServiceUsers(List<ChatUser> offlineServiceUsers) {
		this.offlineServiceUsers = offlineServiceUsers;
	}

	public Boolean getIsJust() {
		return isJust;
	}

	public void setIsJust(Boolean isJust) {
		this.isJust = isJust;
	}

	public Long getCustomerCount() {
		return customerCount;
	}

	public void setCustomerCount(Long customerCount) {
		this.customerCount = customerCount;
	}

	public Long getServiceCount() {
		return serviceCount;
	}

	public void setServiceCount(Long serviceCount) {
		this.serviceCount = serviceCount;
	}

	public List<ChatUser> getCustomerUsers() {
		return customerUsers;
	}

	public void setCustomerUsers(List<ChatUser> customerUsers) {
		this.customerUsers = customerUsers;
	}

	public List<ChatUser> getServiceUsers() {
		return serviceUsers;
	}

	public void setServiceUsers(List<ChatUser> serviceUsers) {
		this.serviceUsers = serviceUsers;
	}

}
