package com.ereniridere.dto.response.message;

import java.time.LocalDateTime;

import lombok.Data;

// SSE üzerinden canlı iletilen mesaj. DtoMessage'dan farkı: chatRoomId de taşır.
// Hem sohbet ekranı (oda filtresi için) hem inbox (hangi oda güncellenecek)
// bu tek şekille çalışır.
@Data
public class DtoRealtimeMessage {
	private Integer id;
	private Integer chatRoomId;
	private Integer senderId;
	private String content;
	private LocalDateTime createdAt;
}
