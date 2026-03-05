package com.ereniridere.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DtoForgotPassword {

	@NotBlank(message = "E-posta alanı boş olamaz")
	@Email(message = "Lütfen geçerli bir e-posta adresi girin")
	private String email;
}
