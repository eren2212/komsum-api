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

	@Override
	public Bucket resolveBucket(Integer userId) {
		// Eğer adamın kovası varsa getir, yoksa yeni bir kova yarat
		return cache.computeIfAbsent(userId, this::newBucket);
	}

	private Bucket newBucket(Integer userId) {
		// SEKTÖR STANDARDI KURAL:
		// Kovanın kapasitesi 5. Ve her 10 saniyede bir kovaya 5 jeton geri koy.
		// Yani adam 10 saniyede en fazla 5 kere beğeni atabilir/çekebilir!
		Bandwidth limit = Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofSeconds(60)).build();

		return Bucket.builder().addLimit(limit).build();
	}

}
