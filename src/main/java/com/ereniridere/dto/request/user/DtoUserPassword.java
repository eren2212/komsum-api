package com.ereniridere.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data

public class DtoUserPassword {

	@NotBlank(message = "Eski şifreniz boş olamaz")
	@Size
	private String oldPassword;

	@NotBlank(message = "Yeni şifreniz boş olamaz")
	private String newPassword;
	@Size(max = 15, min = 8, message = "Maximum 15 minumum 8 karakter olmalı ")

	@NotBlank(message = "Yeni şifreniz boş olamaz")
	@Size(max = 15, min = 8, message = "Maximum 15 minumum 8 karakter olmalı ")
	private String confirmNewPassword;
}
