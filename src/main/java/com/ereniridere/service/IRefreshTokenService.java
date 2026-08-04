package com.ereniridere.service;

import com.ereniridere.entity.User;

/**
 * Refresh token'ların sunucu tarafındaki yaşam döngüsü: kayıt, döndürme
 * (rotation), iptal ve çalıntı token tespiti.
 */
public interface IRefreshTokenService {

	/** Yeni üretilmiş bir refresh token'ı kullanıcı adına kaydeder. */
	void store(String rawToken, User user);

	/**
	 * Yenileme anında token'ı doğrular ve tüketir.
	 *
	 * Kayıtlı değilse ya da süresi geçmişse reddedilir. Zaten iptal edilmiş bir
	 * token tekrar kullanılmışsa bu, token'ın çalındığına işarettir: o
	 * kullanıcının TÜM oturumları kapatılır ve istek reddedilir.
	 *
	 * Başarılıysa kayıt iptal edilir (tek kullanımlık) ve çağıran taraf yeni bir
	 * token üretip {@link #store} ile kaydeder.
	 */
	void consumeForRotation(String rawToken, User user);

	/** Çıkışta tek bir token'ı iptal eder. Bilinmeyen token sessizce yok sayılır. */
	void revoke(String rawToken);

	/** Kullanıcının tüm oturumlarını kapatır (şifre değişimi, çalıntı token). */
	void revokeAllForUser(User user);
}
