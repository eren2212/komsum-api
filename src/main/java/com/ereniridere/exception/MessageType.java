package com.ereniridere.exception;

import lombok.Getter;

@Getter
public enum MessageType {

	NO_RECORD_EXIST("1001", "Kayıt Bulunamadı:"), GENERAL_EXCEPTION("9999", "Herhangi bir hata oldu:"),
	RECORD_ALREADY_EXISTS("1002", "Bu kayıt zaten kullanılıyor:"), COOLDOWN_ACTIVE("1004", "Bekleme süresi:"),
	VALIDATION_FAILED("1006", "Doğrulama hatası:"),
	TOO_MANY_REQUESTS("1008", "Çok hızlı işlem yapıyorsun , biraz yavaşla!"),
	ROOMIO_PROFILE_ALREADY_EXISTS("2001", "Zaten bir Roomio profilin var:"),
	ROOMIO_PROFILE_NOT_FOUND("2002", "Roomio profili bulunamadı:"),
	ROOMIO_ALREADY_SWIPED("2003", "Bu kişiyi zaten değerlendirdin:"),
	ROOMIO_LOCATION_REQUIRED("2005", "Roomio profili için konumunu paylaşmalısın:");

	private String code;

	private String message;

	MessageType(String code, String message) {
		this.code = code;
		this.message = message;
	}

}
