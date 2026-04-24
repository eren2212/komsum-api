package com.ereniridere.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoCreateComment {

	@NotBlank(message = "Yorum boş olamaz !")
	private String content;
}
