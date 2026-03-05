package com.ereniridere.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DtoResetPassword {

	@NotBlank(message = "E-posta alanı boş olamaz")
	@Email(message = "Geçerli bir e-posta girin")
	private String email;

	@NotBlank(message = "Doğrulama kodu boş olamaz")
	private String otp;

	@NotBlank(message = "Yeni şifre boş olamaz")
	@Size(min = 6, message = "Şifre en az 6 karakter olmalı")
	private String newPassword;

	@NotBlank(message = "Şifre tekrarı boş olamaz")
	private String confirmNewPassword;
}