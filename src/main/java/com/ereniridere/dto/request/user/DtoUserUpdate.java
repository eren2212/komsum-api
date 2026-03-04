package com.ereniridere.dto.request.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DtoUserUpdate {

	@Size(min = 2, max = 15, message = "İsim alanı en az 2, en fazla 15 karakter olabilir")
	private String firstname;

	@Size(min = 2, max = 15, message = "Soyisim alanı en az 2, en fazla 15 karakter olabilir")
	private String lastname;

	private Integer neighborhoodId;
}
