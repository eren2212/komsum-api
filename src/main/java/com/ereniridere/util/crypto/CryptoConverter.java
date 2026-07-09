package com.ereniridere.util.crypto;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Hassas metinleri (örn. mesaj içeriği) veritabanına yazarken AES-256-GCM ile
 * şifreler, okurken çözer. JPA {@link AttributeConverter} olduğu için
 * servis/DTO kodu değişmez — şifreleme tamamen şeffaftır ve yalnızca DB
 * sütununa etki eder.
 *
 * Anahtar {@code MESSAGE_ENCRYPTION_KEY} ortam değişkeninden (Base64 kodlu,
 * 256-bit / 32 byte) okunur. Değişken yoksa ilk kullanımda anlaşılır bir hata
 * fırlatılır.
 *
 * Saklama formatı: Base64( 12-byte IV || ciphertext+GCM-tag ).
 */
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

	private static final String ALGORITHM = "AES";
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int IV_LENGTH = 12; // GCM için önerilen IV boyutu
	private static final int GCM_TAG_LENGTH_BITS = 128;

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	// Anahtarı bir kez üretip önbelleğe alıyoruz (lazy init).
	private static volatile SecretKeySpec cachedKey;

	private static SecretKeySpec getKey() {
		SecretKeySpec key = cachedKey;
		if (key == null) {
			synchronized (CryptoConverter.class) {
				key = cachedKey;
				if (key == null) {
					String base64Key = System.getenv("MESSAGE_ENCRYPTION_KEY");
					if (base64Key == null || base64Key.isBlank()) {
						throw new IllegalStateException(
								"MESSAGE_ENCRYPTION_KEY ortam değişkeni tanımlı değil. "
										+ "Base64 kodlu 256-bit (32 byte) bir anahtar verin.");
					}
					byte[] keyBytes;
					try {
						keyBytes = Base64.getDecoder().decode(base64Key.trim());
					} catch (IllegalArgumentException e) {
						throw new IllegalStateException(
								"MESSAGE_ENCRYPTION_KEY geçerli bir Base64 değeri değil.", e);
					}
					if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
						throw new IllegalStateException(
								"MESSAGE_ENCRYPTION_KEY çözüldüğünde 16/24/32 byte olmalı (256-bit önerilir), "
										+ "şu an: " + keyBytes.length + " byte.");
					}
					key = new SecretKeySpec(keyBytes, ALGORITHM);
					cachedKey = key;
				}
			}
		}
		return key;
	}

	@Override
	public String convertToDatabaseColumn(String attribute) {
		if (attribute == null) {
			return null;
		}
		try {
			byte[] iv = new byte[IV_LENGTH];
			SECURE_RANDOM.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			byte[] cipherText = cipher.doFinal(attribute.getBytes(java.nio.charset.StandardCharsets.UTF_8));

			// iv || cipherText birleştir
			byte[] combined = new byte[iv.length + cipherText.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

			return Base64.getEncoder().encodeToString(combined);
		} catch (Exception e) {
			throw new IllegalStateException("Mesaj şifrelenirken hata oluştu.", e);
		}
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		try {
			byte[] combined = Base64.getDecoder().decode(dbData);

			byte[] iv = new byte[IV_LENGTH];
			byte[] cipherText = new byte[combined.length - IV_LENGTH];
			System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
			System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			byte[] plainText = cipher.doFinal(cipherText);

			return new String(plainText, java.nio.charset.StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("Mesaj çözülürken hata oluştu.", e);
		}
	}
}
