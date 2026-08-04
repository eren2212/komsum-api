package com.ereniridere.exception;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Hatanın türü. Mesaj metne çevrilirken kayboluyordu; GlobalExceptionHandler
	 * doğru HTTP durum kodunu (404, 429 ...) seçebilmek için türün kendisine
	 * ihtiyaç duyuyor.
	 */
	private final MessageType messageType;

	public BaseException(ErrorMessage errorMessage) {
		super(errorMessage.prepareMessage());
		this.messageType = errorMessage.getMessageType();
	}

}
