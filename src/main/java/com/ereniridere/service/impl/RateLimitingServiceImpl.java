package com.ereniridere.service.impl;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.ereniridere.service.IRateLimitingService;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

@Service
public class RateLimitingServiceImpl implements IRateLimitingService {

	private final Map<Integer, Bucket> cache = new ConcurrentHashMap<>();

	// Anonim (token'sız) trafiğin kovaları. Anahtar "ip:..." / "login:..." gibi
	// serbest bir String olduğu için ayrı bir map'te tutulur.
	private final Map<String, Bucket> anonymousCache = new ConcurrentHashMap<>();

	// Bellek koruması: her farklı IP/e-posta yeni bir giriş yarattığı için map
	// sınırsız büyüyebilir. Eşiğe gelince tamamen boşaltılır — kaba ama yeterli:
	// en kötü ihtimalle o an limitli olan birkaç kullanıcı bir kez daha hak
	// kazanır, saldırgan ise eşiği aşacak kadar istek atmak zorunda kalır.
	private static final int ANONYMOUS_CACHE_MAX_SIZE = 10_000;

	@Override
	public Bucket resolveBucket(Integer userId) {
		// Eğer adamın kovası varsa getir, yoksa yeni bir kova yarat
		return cache.computeIfAbsent(userId, this::newBucket);
	}

	@Override
	public Bucket resolveBucket(String key, int capacity, Duration refillPeriod) {

		if (anonymousCache.size() >= ANONYMOUS_CACHE_MAX_SIZE) {
			anonymousCache.clear();
		}

		return anonymousCache.computeIfAbsent(key, k -> Bucket.builder()
				.addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, refillPeriod).build()).build());
	}

	private Bucket newBucket(Integer userId) {
		// SEKTÖR STANDARDI KURAL:
		// Kovanın kapasitesi 5. Ve her 10 saniyede bir kovaya 5 jeton geri koy.
		// Yani adam 10 saniyede en fazla 5 kere beğeni atabilir/çekebilir!
		Bandwidth limit = Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofSeconds(60)).build();

		return Bucket.builder().addLimit(limit).build();
	}

}
