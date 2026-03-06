package com.ereniridere.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ereniridere.entity.enums.PostType;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts")
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	// Postlar uzun olabileceği için veritabanında VARCHAR(255) yetmez, TEXT
	// yapıyoruz.
	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	// Eğer adam fotoğraf yüklerse URL'si burada duracak (İsteğe bağlı)
	private String imageUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PostType type;

	// 1. KANCA: Bu postu KİM attı?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User author;

	// 2. KANCA: Bu post HANGİ MAHALLEYE atıldı?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "neighborhood_id", nullable = false)
	private Neighborhood neighborhood;

	// Post atıldığı an saati otomatik olarak kaydeder, bizim elle girmemize gerek
	// kalmaz!
	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	// Kullanıcı postunu silerse veritabanından tamamen uçurmak yerine bunu false
	// yaparız (Soft Delete)
	@Builder.Default
	private boolean isActive = true;
}