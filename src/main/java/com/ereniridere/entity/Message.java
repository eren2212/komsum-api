package com.ereniridere.entity;

import java.time.LocalDateTime;

import com.ereniridere.util.crypto.CryptoConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

	// Mesaj içeriği veritabanında AES-GCM ile şifreli tutulur (at-rest).
	// Şifreli + Base64 metin orijinalden büyük olduğu için TEXT kullanıyoruz.
	@Convert(converter = CryptoConverter.class)
	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	// MVP için çok mühim değil ama ileride "Görüldü" tiki yapmak istersen hayat
	// kurtarır
	@Column(name = "is_read")
	private boolean isRead = false;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();
}