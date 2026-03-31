package com.ereniridere.service.impl;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupabaseStorageServiceImpl {

	@Value("${supabase.url}")
	private String supabaseUrl;

	@Value("${supabase.key}")
	private String supabaseKey;

	@Value("${supabase.bucket}")
	private String bucketName;

	// Resim yükleme ve URL dönme metodu
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

			// SİHİRLİ DOKUNUŞ BURASI: Dosya yolunu (Path) klasör ismiyle birleştiriyoruz
			// Örn: "merchants/550e8400..._profil.jpg"
			String filePath = folderName + "/" + fileName;

			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", "Bearer " + supabaseKey);
			headers.set("apikey", supabaseKey);
			headers.setContentType(MediaType.valueOf(file.getContentType()));

			HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

			// Yükleme URL'sine filePath'i veriyoruz
			String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + filePath;

			ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity,
					String.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				// Dönüş URL'sinde de artık klasör adı var!
				return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + filePath;
			} else {
				throw new BaseException(
						new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Supabase'e yüklerken bir sorun oluştu!"));
			}

		} catch (IOException e) {
			throw new BaseException(
					new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Dosya okunurken hata oluştu: " + e.getMessage()));
		}
	}
}