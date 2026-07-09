package com.ereniridere.service.impl;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.service.IStorageService;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * MinIO (S3 uyumlu) tabanlı dosya deposu servisi. Eski SupabaseStorageServiceImpl'in yerini alır.
 * Yükleme S3 SDK ile {@code storage.endpoint}'e yapılır; döndürülen URL ise telefondan
 * erişilebilen {@code storage.public-url} tabanlı public adrestir (bucket public-read olmalı).
 */
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements IStorageService {

	private final S3Client s3Client;

	@Value("${storage.bucket}")
	private String bucketName;

	@Value("${storage.public-url}")
	private String publicUrl;

	@Override
	public String uploadImage(MultipartFile file, String folderName) {

		if (file.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Kanzi boş dosya yükleyemezsin!"));
		}
		if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Sadece resim dosyası yükleyebilirsin!"));
		}

		try {
			String fileExtension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
			String fileName = UUID.randomUUID().toString() + fileExtension;

			// Depo içindeki anahtar (key): klasör + dosya adı. Örn: "avatars/12/550e8400....jpg"
			String objectKey = folderName + "/" + fileName;

			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(objectKey)
					.contentType(file.getContentType())
					.build();

			s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

			// Telefonun erişebileceği public URL: <public-url>/<bucket>/<key>
			// Sondaki olası "/" temizlenir ki çift slash oluşmasın.
			String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
			return base + "/" + bucketName + "/" + objectKey;

		} catch (S3Exception e) {
			throw new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION,
					"MinIO'ya yüklerken bir sorun oluştu: " + e.awsErrorDetails().errorMessage()));
		} catch (IOException e) {
			throw new BaseException(
					new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Dosya okunurken hata oluştu: " + e.getMessage()));
		}
	}
}
