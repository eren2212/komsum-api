package com.ereniridere.entity.enums;

/**
 * Günlük "Komşu Görevi" tipleri. Sadece backend'de doğrulanabilen (gerçek bir
 * aksiyona bağlı) görevler burada yer alır.
 */
public enum TaskType {
	CREATE_POST, // Mahalle akışına gönderi paylaş
	JOIN_EVENT, // Bir etkinliğe katıl
	WRITE_COMMENT // Bir gönderiye yorum yaz
}
