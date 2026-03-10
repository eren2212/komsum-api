package com.ereniridere.exception;

import lombok.Getter;

@Getter
public enum MessageType {

	NO_RECORD_EXIST("1001", "Kayıt Bulunamadı:"), GENERAL_EXCEPTION("9999", "Herhangi bir hata oldu:"),
	RECORD_ALREADY_EXISTS("1002", "Bu kayıt zaten kullanılıyor:"), COOLDOWN_ACTIVE("1004", "Bekleme süresi:"),
	VALIDATION_FAILED("1006", "Doğrulama hatası:"),
	TOO_MANY_REQUESTS("1008", "Çok hızlı işlem yapıyorsun , biraz yavaşla!");

	private String code;

	private String message;

	MessageType(String code, String message) {
		this.code = code;
		this.message = message;
	}

}
