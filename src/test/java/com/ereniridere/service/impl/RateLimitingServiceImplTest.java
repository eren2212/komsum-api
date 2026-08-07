package com.ereniridere.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.bucket4j.Bucket;

class RateLimitingServiceImplTest {

	private final RateLimitingServiceImpl service = new RateLimitingServiceImpl();

	@Test
	void resolveBucket_byUserId_allowsUpToCapacityThenBlocks() {
		Bucket bucket = service.resolveBucket(42);

		// newBucket() kapasitesi 5 (bkz. RateLimitingServiceImpl#newBucket)
		for (int i = 0; i < 5; i++) {
			assertThat(bucket.tryConsume(1)).as("istek #%d kabul edilmeli", i + 1).isTrue();
		}
		assertThat(bucket.tryConsume(1)).as("kapasite aşılınca reddedilmeli").isFalse();
	}

	@Test
	void resolveBucket_byUserId_sameUserReusesSameBucket() {
		Bucket first = service.resolveBucket(7);
		Bucket second = service.resolveBucket(7);

		assertThat(first).isSameAs(second);
	}

	@Test
	void resolveBucket_byUserId_differentUsersGetIndependentBuckets() {
		Bucket userA = service.resolveBucket(1);
		Bucket userB = service.resolveBucket(2);

		for (int i = 0; i < 5; i++) {
			userA.tryConsume(1);
		}
		assertThat(userA.tryConsume(1)).isFalse();
		// userA'nın kovası boşalmış olsa da userB kendi kovasından tüketebilmeli.
		assertThat(userB.tryConsume(1)).isTrue();
	}

	@Test
	void resolveBucket_byKey_respectsGivenCapacityAndIsPerKey() {
		Bucket chatBucket = service.resolveBucket("chat:99", 3, Duration.ofSeconds(10));

		assertThat(chatBucket.tryConsume(1)).isTrue();
		assertThat(chatBucket.tryConsume(1)).isTrue();
		assertThat(chatBucket.tryConsume(1)).isTrue();
		assertThat(chatBucket.tryConsume(1)).isFalse();

		// Farklı bir anahtar bağımsız bir kovaya sahip olmalı.
		Bucket otherBucket = service.resolveBucket("chat:100", 3, Duration.ofSeconds(10));
		assertThat(otherBucket.tryConsume(1)).isTrue();
	}
}
