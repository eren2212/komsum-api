package com.ereniridere.dto.request.post;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class DtoUpdatePost {

	@NotBlank(message = "Post gönderisi boş olamaz ")
	private String content;
}
