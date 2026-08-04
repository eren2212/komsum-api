package com.ereniridere.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.RefreshToken;
import com.ereniridere.entity.User;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.RefreshTokenRepository;
import com.ereniridere.security.jwt.JwtService;
import com.ereniridere.service.IRefreshTokenService;

@Service
public class RefreshTokenServiceImpl implements IRefreshTokenService {

	private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private JwtService jwtService;

	@Override
	@Transactional
	public void store(String rawToken, User user) {
		refreshTokenRepository.save(RefreshToken.builder().tokenHash(hash(rawToken)).user(user)
				.expiresAt(expiryOf(rawToken)).createdAt(LocalDateTime.now()).build());
	}

	@Override
	@Transactional
	public void consumeForRotation(String rawToken, User user) {

		Optional<RefreshToken> stored = refreshTokenRepository.findByTokenHash(hash(rawToken));

		if (stored.isEmpty()) {
			// İmzası geçerli ama kaydı olmayan token: çıkış yapılmış ya da
			// veritabanından temizlenmiş bir oturum.
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Geçersiz refresh token"));
		}

		RefreshToken token = stored.get();

		// ÇALINTI TOKEN TESPİTİ: Bu token daha önce kullanılıp iptal edilmiş.
		// Meşru istemci her yenilemede yenisini aldığı için eskisini bir daha
		// göndermez — aynı token'ın ikinci kez gelmesi, kopyasının başkasının
		// elinde olduğu anlamına gelir. Kimin gerçek kullanıcı olduğunu
		// bilemediğimiz için tüm oturumları kapatıyoruz.
		if (token.getRevokedAt() != null) {
			log.warn("Kullanilmis refresh token yeniden kullanildi (userId={}), tum oturumlar kapatiliyor.",
					user.getId());
			revokeAllForUser(user);
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED,
					"Oturumun güvenliği nedeniyle sonlandırıldı, lütfen tekrar giriş yap"));
		}

		if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Refresh token süresi dolmuş"));
		}

		// Kayıt başka bir kullanıcıya aitse (token'daki subject ile eşleşmiyorsa)
		// hiç devam etme.
		if (!token.getUser().getId().equals(user.getId())) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Geçersiz refresh token"));
		}

		// Tek kullanımlık: yenileme başarılıysa bu token bir daha çalışmaz.
		token.setRevokedAt(LocalDateTime.now());
		refreshTokenRepository.save(token);
	}

	@Override
	@Transactional
	public void revoke(String rawToken) {
		refreshTokenRepository.findByTokenHash(hash(rawToken)).filter(t -> t.getRevokedAt() == null).ifPresent(t -> {
			t.setRevokedAt(LocalDateTime.now());
			refreshTokenRepository.save(t);
		});
	}

	@Override
	@Transactional
	public void revokeAllForUser(User user) {
		refreshTokenRepository.revokeAllByUser(user, LocalDateTime.now());
	}

	/**
	 * Süresi dolmuş kayıtları günde bir kez siler. İptal edilmiş ama henüz
	 * süresi dolmamış kayıtlar KALIR — çalıntı token tespiti onlara bakıyor.
	 */
	@Scheduled(cron = "0 30 3 * * *")
	@Transactional
	public void purgeExpiredTokens() {
		int deleted = refreshTokenRepository.deleteAllExpiredBefore(LocalDateTime.now());
		if (deleted > 0) {
			log.info("Suresi dolmus {} refresh token kaydi temizlendi.", deleted);
		}
	}

	/**
	 * Ham token yerine SHA-256 özeti saklanır: veritabanı sızsa bile
	 * kayıtlardan çalışan bir token geri üretilemez.
	 */
	private String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			// SHA-256 her JVM'de zorunlu — buraya asla düşmemeli.
			throw new IllegalStateException("SHA-256 algoritması bulunamadı", e);
		}
	}

	/** Kaydın son kullanma tarihi, token'ın kendi exp claim'inden alınır. */
	private LocalDateTime expiryOf(String rawToken) {
		Instant exp = jwtService.extractExpiration(rawToken).toInstant();
		return LocalDateTime.ofInstant(exp, ZoneId.systemDefault());
	}
}
