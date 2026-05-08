package com.ereniridere.dto.request.message;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DtoSendMessage {

	@NotBlank(message = "Boş mesaj gönderemezsin!")
	private String content;

}