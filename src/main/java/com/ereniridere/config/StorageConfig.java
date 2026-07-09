package com.ereniridere.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * MinIO (S3 uyumlu) dosya deposuna bağlanan S3Client'ı kurar.
 * Supabase Storage yerine geçer; ayarlar application.yml'deki storage.* bloğundan gelir.
 */
@Configuration
public class StorageConfig {

	@Value("${storage.endpoint}")
	private String endpoint;

	@Value("${storage.access-key}")
	private String accessKey;

	@Value("${storage.secret-key}")
	private String secretKey;

	@Value("${storage.region}")
	private String region;

	@Value("${storage.path-style-access:true}")
	private boolean pathStyleAccess;

	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
				.endpointOverride(URI.create(endpoint))
				.region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(accessKey, secretKey)))
				// MinIO yol-stili adresleme gerektirir (bucket adı host yerine path'te).
				.forcePathStyle(pathStyleAccess)
				.build();
	}
}
