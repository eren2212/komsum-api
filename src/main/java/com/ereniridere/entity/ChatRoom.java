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
@Table(name = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	// Sohbeti başlatan kişi
	@ManyToOne
	@JoinColumn(name = "user1_id", nullable = false)
	private User user1;

	// Mesaj atılan kişi
	@ManyToOne
	@JoinColumn(name = "user2_id", nullable = false)
	private User user2;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "last_message_at")
	private LocalDateTime lastMessageAt = LocalDateTime.now();

	// Son mesaj önizlemesi de mesaj içeriğini barındırdığı için şifreli tutulur.
	@Convert(converter = CryptoConverter.class)
	@Column(name = "last_message_content", columnDefinition = "TEXT")
	private String lastMessageContent;
}