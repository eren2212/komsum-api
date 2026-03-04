package com.ereniridere.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class DtoRegisterRequest {

	@NotBlank(message = "İsim olmak zorunda")
	@Size(max = 15, message = "Maximum 15 minumum 8 karakter olmalı ")
	private String firstname;

	@NotBlank(message = "Soy isim olmak zorunda")
	@Size(max = 15, message = "Maximum 15 minumum 8 karakter olmalı ")
	private String lastname;

	@NotBlank(message = "Email  olmak zorunda")
	@Email(message = "Geçerli bir email olmak zorunda", regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")
	private String email;

	@NotBlank(message = "Şifre zorunlu")
	@Size(max = 15, min = 8, message = "Maximum 15 minumum 8 karakter olmalı ")
	private String password;

	@Positive
	@NotNull
	private Integer neighborhoodId;
}
