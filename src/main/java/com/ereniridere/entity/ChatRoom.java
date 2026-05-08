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

	@Column(name = "last_message_content", length = 200)
	private String lastMessageContent;
}