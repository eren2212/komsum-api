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
// SENIOR DOKUNUŞU: Bir kullanıcı bir postu sadece 1 kere beğenebilir! 
// Veritabanı (PostgreSQL) ikinci denemede otomatik tokat atar.
@Table(name = "post_likes", uniqueConstraints = { @UniqueConstraint(columnNames = { "post_id", "user_id" }) })
public class PostLike {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	// 1. Kanca: Hangi Post beğenildi?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	// 2. Kanca: Kim beğendi?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// Beğeninin atıldığı anı kaydeder (İleride "Ahmet gönderini 2 dakika önce
	// beğendi" bildirimi atmak için)
	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
}