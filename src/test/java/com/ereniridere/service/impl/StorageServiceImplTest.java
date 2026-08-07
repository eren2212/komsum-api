package com.ereniridere.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.ereniridere.exception.BaseException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class StorageServiceImplTest {

	private static final byte[] JPEG_MAGIC_BYTES = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02 };

	private S3Client s3Client;
	private StorageServiceImpl storageService;

	@BeforeEach
	void setUp() {
		s3Client = mock(S3Client.class);
		storageService = new StorageServiceImpl(s3Client);
		ReflectionTestUtils.setField(storageService, "bucketName", "komsum-images");
		ReflectionTestUtils.setField(storageService, "publicUrl", "https://cdn.example.com");
	}

	@Test
	void uploadImage_rejectsEmptyFile() {
		MockMultipartFile empty = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

		assertThatThrownBy(() -> storageService.uploadImage(empty, "avatars")).isInstanceOf(BaseException.class);
	}

	@Test
	void uploadImage_rejectsFileOverSizeLimit() {
		byte[] tooBig = new byte[6 * 1024 * 1024]; // 6 MB > 5 MB sınırı
		MockMultipartFile big = new MockMultipartFile("file", "big.jpg", "image/jpeg", tooBig);

		assertThatThrownBy(() -> storageService.uploadImage(big, "avatars")).isInstanceOf(BaseException.class);
	}

	@Test
	void uploadImage_rejectsContentThatIsNotARecognizedImage_regardlessOfDeclaredContentType() {
		// İstemci "image/jpeg" dese de gerçek baytlar bir görsel değil (saldırgan senaryosu).
		MockMultipartFile fakeImage = new MockMultipartFile("file", "not-really.jpg", "image/jpeg",
				"<script>alert(1)</script>".getBytes());

		assertThatThrownBy(() -> storageService.uploadImage(fakeImage, "avatars")).isInstanceOf(BaseException.class);
	}

	@Test
	void uploadImage_acceptsValidJpegAndReturnsPublicUrl() {
		when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
				.thenReturn(PutObjectResponse.builder().build());

		MockMultipartFile jpeg = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", JPEG_MAGIC_BYTES);

		String url = storageService.uploadImage(jpeg, "avatars");

		assertThat(url).startsWith("https://cdn.example.com/komsum-images/avatars/");
		assertThat(url).endsWith(".jpg");
		verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
	}
}
