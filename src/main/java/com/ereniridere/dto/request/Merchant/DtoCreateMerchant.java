package com.ereniridere.dto.request.Merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

	// Dükkanın harita konumu (SPONSORED post radius filtresi bunun üzerinden çalışır).
	// Esnaf dükkanını haritadan işaretlediği için zorunlu.
	@NotNull(message = "Dükkanını haritadan işaretlemelisin!")
	private Double latitude;

	@NotNull(message = "Dükkanını haritadan işaretlemelisin!")
	private Double longitude;
}