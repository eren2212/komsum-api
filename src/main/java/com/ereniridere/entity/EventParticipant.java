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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_participants")
@Data
@NoArgsConstructor
public class EventParticipant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "joined_at")
	private LocalDateTime joinedAt = LocalDateTime.now();
}