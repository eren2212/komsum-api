package com.ereniridere.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ereniridere.entity.RefreshToken;
import com.ereniridere.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/**
	 * Kullanıcının henüz iptal edilmemiş tüm refresh token'larını iptal eder.
	 * Şifre sıfırlamada ve çalıntı token tespitinde tüm oturumları kapatmak için.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE RefreshToken rt SET rt.revokedAt = :now WHERE rt.user = :user AND rt.revokedAt IS NULL")
	int revokeAllByUser(@Param("user") User user, @Param("now") LocalDateTime now);

	/** Süresi geçmiş kayıtları temizler (tablo sonsuza kadar büyümesin). */
	@Modifying
	@Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :before")
	int deleteAllExpiredBefore(@Param("before") LocalDateTime before);

	/**
	 * Hesap silinirken çağrılır: kullanıcı satırı silinmeden önce ona bağlı
	 * token kayıtları gitmezse user_id foreign key'i silmeyi engeller.
	 */
	@Modifying
	@Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
	int deleteAllByUserId(@Param("userId") Integer userId);
}
