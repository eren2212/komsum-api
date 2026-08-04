package com.ereniridere.service;

import java.time.Duration;

import io.github.bucket4j.Bucket;

public interface IRateLimitingService {

	public Bucket resolveBucket(Integer userId);

	/**
	 * Giriş yapmamış (anonim) trafiği sınırlamak için serbest anahtarlı kova.
	 * userId olmayan uçlarda (login, register, şifre sıfırlama) IP ya da e-posta
	 * anahtar olarak kullanılır.
	 *
	 * @param key          kova anahtarı, ör. "ip:1.2.3.4" veya "login:ali@x.com"
	 * @param capacity     periyot başına izin verilen istek sayısı
	 * @param refillPeriod kovanın tamamen dolduğu süre
	 */
	public Bucket resolveBucket(String key, int capacity, Duration refillPeriod);

}
