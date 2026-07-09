package com.ereniridere.dto.request.serviceprovider;

import com.ereniridere.entity.enums.ServiceCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DtoCreateServiceProvider {

	@NotBlank(message = "Başlık boş olamaz (Örn: Matematik Öğretmeni)")
	private String title;

	@NotNull(message = "Meslek kategorisi seçmelisin")
	private ServiceCategory category;

	@NotBlank(message = "Telefon numarası zorunludur")
	private String phone;

	@NotBlank(message = "Açık adres girmelisin")
	private String address;

	private String description; // Zorunlu değil

	private Integer experienceYears; // Deneyim yılı — opsiyonel

	private String priceInfo; // Ücret bilgisi — opsiyonel serbest metin

	private Boolean available; // Müsaitlik — gelmezse serviste true kabul edilir

	// Harita konumu — ustanın hizmet verdiği bölgeyi haritadan işaretler (zorunlu).
	@NotNull(message = "Konumunu haritadan işaretlemelisin!")
	private Double latitude;

	@NotNull(message = "Konumunu haritadan işaretlemelisin!")
	private Double longitude;
}
