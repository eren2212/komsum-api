package com.ereniridere.service.impl;

import java.io.IOException;
import java.util.Map;
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

	/** Uygulama içi boyut sınırı. application.yml'deki multipart limiti bundan biraz yüksek. */
	private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

	/**
	 * İzin verilen görsel türleri ve dosya uzantıları.
	 *
	 * Tür, dosyanın İÇERİĞİNDEN (magic byte) tespit edilir; istemcinin
	 * gönderdiği Content-Type ve dosya adı dikkate ALINMAZ — ikisi de
	 * saldırgan tarafından serbestçe belirlenebilir.
	 *
	 * SVG bilerek listede yok: XML tabanlıdır, script barındırabilir ve
	 * bucket'tan aynı origin üzerinden servis edildiğinde saklı XSS'e döner.
	 */
	private static final Map<String, String> ALLOWED_TYPES = Map.of("image/jpeg", ".jpg", "image/png", ".png",
			"image/webp", ".webp");

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

		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED,
					"Görsel en fazla 5 MB olabilir, daha küçük bir dosya seçin."));
		}

		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException e) {
			throw new BaseException(
					new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Dosya okunurken hata oluştu: " + e.getMessage()));
		}

		// Gerçek tür dosyanın kendi baytlarından belirlenir.
		String detectedType = detectImageType(bytes);
		if (detectedType == null) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Sadece JPEG, PNG veya WebP görsel yükleyebilirsin!"));
		}

		try {
			// Uzantı da tespit edilen türden türetilir; istemcinin gönderdiği
			// dosya adına hiç dokunulmaz (uzantısız ad, çift uzantı, yol
			// karakteri gibi sürprizler böylece imkânsız hâle gelir).
			String fileName = UUID.randomUUID().toString() + ALLOWED_TYPES.get(detectedType);

			// Depo içindeki anahtar (key): klasör + dosya adı. Örn: "avatars/12/550e8400....jpg"
			String objectKey = folderName + "/" + fileName;

			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(objectKey)
					.contentType(detectedType)
					.build();

			s3Client.putObject(request, RequestBody.fromBytes(bytes));

			// Telefonun erişebileceği public URL: <public-url>/<bucket>/<key>
			// Sondaki olası "/" temizlenir ki çift slash oluşmasın.
			String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
			return base + "/" + bucketName + "/" + objectKey;

		} catch (S3Exception e) {
			throw new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION,
					"MinIO'ya yüklerken bir sorun oluştu: " + e.awsErrorDetails().errorMessage()));
		}
	}

	/**
	 * Dosyanın ilk baytlarına (magic number) bakarak gerçek görsel türünü
	 * döndürür. Tanınmayan/izin verilmeyen tür için null.
	 */
	private String detectImageType(byte[] bytes) {

		// JPEG: FF D8 FF
		if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
			return "image/jpeg";
		}

		// PNG: 89 50 4E 47 0D 0A 1A 0A
		if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
				&& (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A && (bytes[6] & 0xFF) == 0x1A
				&& (bytes[7] & 0xFF) == 0x0A) {
			return "image/png";
		}

		// WebP: "RIFF" .... "WEBP"
		if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
				&& bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
			return "image/webp";
		}

		return null;
	}
}
