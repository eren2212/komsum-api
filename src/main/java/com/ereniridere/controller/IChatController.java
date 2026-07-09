package com.ereniridere.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ereniridere.dto.request.message.DtoSendMessage;
import com.ereniridere.dto.request.message.DtoStartChat;
import com.ereniridere.dto.response.message.DtoChatRoom;
import com.ereniridere.dto.response.message.DtoMessage;
import com.ereniridere.entity.RootEntity;

public interface IChatController {

	public RootEntity<DtoChatRoom> startChat(DtoStartChat request);

	public RootEntity<DtoMessage> sendMessage(Integer roomId, DtoSendMessage request);

	public RootEntity<Page<DtoMessage>> getChatMessages(Integer roomId, int pageNo, int pageSize);

	public RootEntity<Page<DtoChatRoom>> getMyChatRooms(int pageNo, int pageSize);

	public RootEntity<Void> markAsRead(Integer roomId);

	// Canlı mesaj akışı (SSE). RootEntity zarfı kullanmaz; doğrudan SseEmitter döner.
	public SseEmitter stream();

}