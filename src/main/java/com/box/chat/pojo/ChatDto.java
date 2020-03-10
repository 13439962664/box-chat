package com.box.chat.pojo;

import java.io.Serializable;

public class ChatDto<P, R> implements Serializable {
	private static final long serialVersionUID = 1695176968770791034L;
	private ChatRequest<P> request = new ChatRequest<P>();
	private ChatResponse<R> response = new ChatResponse<R>();

	public ChatDto() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ChatDto(ChatRequest<P> request, ChatResponse<R> response) {
		super();
		this.request = request;
		this.response = response;
	}

	public ChatRequest<P> getRequest() {
		return request;
	}

	public void setRequest(ChatRequest<P> request) {
		this.request = request;
	}

	public ChatResponse<R> getResponse() {
		return response;
	}

	public void setResponse(ChatResponse<R> response) {
		this.response = response;
	}
}
