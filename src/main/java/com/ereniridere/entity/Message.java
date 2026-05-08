package com.ereniridere.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	// Bu mesaj hangi odaya atıldı?
	@ManyToOne
	@JoinColumn(name = "chat_room_id", nullable = false)
	private ChatRoom chatRoom;

	// Mesajı kim attı? (Odadaki user1 mi, user2 mi?)
	@ManyToOne
	@JoinColumn(name = "sender_id", nullable = false)
	private User sender;

	@Column(nullable = false, length = 1000)
	private String content;

	// MVP için çok mühim değil ama ileride "Görüldü" tiki yapmak istersen hayat
	// kurtarır
	@Column(name = "is_read")
	private boolean isRead = false;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();
}