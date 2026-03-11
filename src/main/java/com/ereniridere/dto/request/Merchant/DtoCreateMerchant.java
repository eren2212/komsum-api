package com.ereniridere.dto.request.Merchant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DtoCreateMerchant {

	@NotBlank(message = "Dükkan adı boş olamaz")
	private String shopName;

	@NotBlank(message = "Kategori seçmelisin (Örn: Gıda, Tesisat)")
	private String category;

	@NotBlank(message = "Telefon numarası zorunludur")
	private String phone;

	@NotBlank(message = "Açık adres girmelisin")
	private String address;

	private String description; // Zorunlu değil
}