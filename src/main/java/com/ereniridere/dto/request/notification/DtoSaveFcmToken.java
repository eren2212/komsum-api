package com.ereniridere.dto.request.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DtoSaveFcmToken {

	@NotBlank(message = "Token boş olamaz")
	private String token;
}
