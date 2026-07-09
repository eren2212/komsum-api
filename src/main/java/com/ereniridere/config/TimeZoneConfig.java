package com.ereniridere.config;

import java.util.TimeZone;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Uygulamanın varsayılan saat dilimini Türkiye'ye (Europe/Istanbul, UTC+3)
 * sabitler.
 *
 * Sorun: Sunucu (Docker) varsayılan olarak UTC çalışıyordu. Bu yüzden
 * {@code LocalDateTime.now()} ve {@code @CreationTimestamp} ile üretilen
 * tarihler 3 saat geriden geliyordu; mobil taraf bu offset'siz tarihleri
 * cihazın yerel saati sanınca mesaj/post/yorumda ~3 saatlik sapma oluşuyordu.
 *
 * Çözüm: JVM varsayılan saat dilimini Europe/Istanbul yaparak tüm zaman
 * üretimini Türkiye yerel saatine çekiyoruz. Sütunlar offset'siz TIMESTAMP
 * olduğu için değer aynen yazılır ve istemci doğru yorumlar.
 */
@Configuration
public class TimeZoneConfig {

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"));
	}
}
