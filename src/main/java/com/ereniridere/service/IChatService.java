package com.ereniridere.service;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.message.DtoSendMessage;
import com.ereniridere.dto.request.message.DtoStartChat;
import com.ereniridere.dto.response.message.DtoChatRoom;
import com.ereniridere.dto.response.message.DtoMessage;

public interface IChatService {

	// 1. Sohbet Başlat (Eskisi varsa getirir, yoksa yeni açar)
	public DtoChatRoom startChat(Integer currentUserId, DtoStartChat request);

	// 2. Mesaj Gönder
	public DtoMessage sendMessage(Integer currentUserId, Integer roomId, DtoSendMessage request);

	// 3. Odanın Mesaj Geçmişini Çek (Sayfalamalı)
	public Page<DtoMessage> getChatMessages(Integer currentUserId, Integer roomId, int pageNo, int pageSize);

	// 4. Benim Gelen Kutum (Konuştuğum Tüm Kişiler)
	public Page<DtoChatRoom> getMyChatRooms(Integer currentUserId, int pageNo, int pageSize);

	// 5. Odanın Mesajlarını Okundu Olarak İşaretle
	public void markAsRead(Integer currentUserId, Integer roomId);

}