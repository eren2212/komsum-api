package com.ereniridere.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ereniridere.dto.response.message.DtoRealtimeMessage;
import com.ereniridere.dto.response.roomio.DtoRoomioMatchPush;

public interface IRealtimeChatService {

	// Kullanıcı canlı mesaj akışına abone olur (SSE bağlantısı açılır).
	public SseEmitter subscribe(Integer userId);

	// Belirli bir kullanıcının tüm açık bağlantılarına canlı mesaj gönderir.
	public void sendToUser(Integer userId, DtoRealtimeMessage payload);

	// Belirli bir kullanıcının tüm açık bağlantılarına canlı Roomio eşleşme bildirimi gönderir.
	public void sendRoomioMatchToUser(Integer userId, DtoRoomioMatchPush payload);

}
