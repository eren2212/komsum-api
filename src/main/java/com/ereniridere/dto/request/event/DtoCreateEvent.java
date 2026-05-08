package com.ereniridere.dto.request.event;

import java.time.LocalDateTime;

import com.ereniridere.entity.enums.EventCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DtoCreateEvent {

	@NotBlank(message = "Etkinlik başlığı boş olamaz!")
	private String title;

	private String description;

	private String imageUrl;

	@NotNull(message = "Kategori seçmelisin!")
	private EventCategory category;

	@NotNull(message = "Etkinlik tarihi boş olamaz!")
	private LocalDateTime eventDate;

	@NotBlank(message = "Etkinlik konumu boş olamaz!")
	private String location;

	@NotNull(message = "Haritadan konum seçilmelidir!")
	private Double latitude;

	@NotNull(message = "Haritadan konum seçilmelidir!")
	private Double longitude;

	private String priceText; // Ücretsizse boş gelebilir veya "Ücretsiz" yazabilir
}