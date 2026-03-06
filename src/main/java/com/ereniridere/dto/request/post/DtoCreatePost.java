package com.ereniridere.dto.request.post;

import com.ereniridere.entity.enums.PostType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DtoCreatePost {

	@NotBlank(message = "Gönderi içeriği boş olamaz kanzi!")
	private String content;

	// Resim zorunlu değil, o yüzden @NotBlank koymuyoruz
	private String imageUrl;

	@NotNull(message = "Gönderi tipi seçilmelidir (STANDARD, HELP_REQUEST, SPONSORED)")
	private PostType type;
}