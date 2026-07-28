package com.ereniridere.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roomio_matches")
public class RoomioMatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_a_id", nullable = false)
	private User userA;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_b_id", nullable = false)
	private User userB;

	// ChatServiceImpl.startChat() ile alınan/oluşturulan odanın id'si — ayrıca
	// @ManyToOne ilişkisine gerek yok, tek yönlü referans yeterli.
	@Column(name = "chat_room_id", nullable = false)
	private Integer chatRoomId;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
}
