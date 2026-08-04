package com.ereniridere.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dağıtılmış refresh token'ların kaydı.
 *
 * JWT'nin kendisi stateless olduğu için imzası geçerli bir token'ı iptal etmenin
 * başka yolu yok: uzaktan çıkış yaptırmak, çalınan bir token'ı geçersiz kılmak
 * ya da şifre değişiminde oturumları kapatmak ancak sunucu tarafında bir kayıt
 * tutmakla mümkün.
 *
 * Token'ın kendisi DEĞİL, SHA-256 özeti saklanır: veritabanı sızsa bile
 * kayıtlardan çalışan bir token üretilemez (tıpkı şifreler gibi).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens", indexes = {
		// Her yenileme isteği token özetiyle arama yapıyor — indekssiz olmaz.
		@Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash"),
		// Kullanıcının tüm oturumlarını toplu iptal ederken kullanılır.
		@Index(name = "idx_refresh_tokens_user_id", columnList = "user_id") })
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	/** Ham token'ın SHA-256 özeti (Base64). Ham değer hiçbir yerde saklanmaz. */
	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	/** Dolu ise token artık kullanılamaz (yenilendi, çıkış yapıldı veya iptal edildi). */
	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	/** Kayıt hâlâ kullanılabilir mi? */
	public boolean isUsable() {
		return revokedAt == null && expiresAt.isAfter(LocalDateTime.now());
	}
}
