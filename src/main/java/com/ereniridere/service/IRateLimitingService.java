package com.ereniridere.service;

import io.github.bucket4j.Bucket;

public interface IRateLimitingService {

	public Bucket resolveBucket(Integer userId);

}
