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
@Table(name = "comments")
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	// Yorum metni (Uzun olabileceği için TEXT yapıyoruz)
	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	// 1. Kanca: Bu yorum hangi Post'un altına yapıldı?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	// 2. Kanca: Bu yorumu kim yazdı?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User author;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	// Yorumu silersek false yapacağız (Soft Delete)
	@Builder.Default
	private boolean isActive = true;
}