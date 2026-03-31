package com.ereniridere.dto.request.market;

import java.math.BigDecimal;

import com.ereniridere.entity.enums.ListingType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DtoCreateListing {

	@NotBlank(message = "Kanzi ilan başlığı boş olamaz!")
	private String title;

	@NotBlank(message = "Lütfen ürünün en az bir fotoğrafını ekleyin.")
	private String imageUrl; // Supabase'den aldığın o meşhur URL buraya gelecek!

	@NotNull(message = "İlan türü (Satılık/Takas) seçilmelidir.")
	private ListingType type;

	// SENIOR DOKUNUŞU: Buraya @NotNull KOYMUYORUZ!
	// Çünkü adam "Hediye / Takas" seçerse fiyat girmeyecek (null olacak).
	// Bunun kontrolünü Servis katmanında if bloğuyla "Satılıksa fiyat zorunludur"
	// diye manuel yapacağız.
	private BigDecimal price;

	@NotBlank(message = "Lütfen bir kategori seçin.")
	private String category;

}
