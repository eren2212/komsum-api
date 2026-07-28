package com.ereniridere.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ereniridere.entity.enums.RoomioSwipeAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roomio_swipes", uniqueConstraints = @UniqueConstraint(columnNames = { "swiper_id", "swiped_id" }))
public class RoomioSwipe {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "swiper_id", nullable = false)
	private User swiper;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "swiped_id", nullable = false)
	private User swiped;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RoomioSwipeAction action;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
}
