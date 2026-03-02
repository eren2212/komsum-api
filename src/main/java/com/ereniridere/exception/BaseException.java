package com.ereniridere.exception;

import lombok.Data;

@Data

public class BaseException extends RuntimeException {

	public BaseException(ErrorMessage errorMessage) {
		super(errorMessage.prepareMessage());
	}

}
