package com.ereniridere.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DtoUpdateComment {
	@NotBlank(message = "Yorum boş olamaz kanzi!")
	private String content;
}