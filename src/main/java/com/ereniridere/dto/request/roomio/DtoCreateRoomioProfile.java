package com.ereniridere.dto.request.roomio;

import java.util.List;

import com.ereniridere.entity.enums.RoomioGender;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DtoCreateRoomioProfile {

	private String bio; // Zorunlu değil

	@NotNull(message = "Yaşını girmelisin")
	@Min(value = 18, message = "Roomio'ya en az 18 yaşında katılabilirsin")
	@Max(value = 100, message = "Geçerli bir yaş girmelisin")
	private Integer age;

	@NotNull(message = "Cinsiyetini seçmelisin")
	private RoomioGender gender;

	// Roomio profilinin konumu (aday akışı radius/ST_DWithin bu üzerinden çalışır).
	@NotNull(message = "Roomio profili için konumunu paylaşmalısın!")
	private Double latitude;

	@NotNull(message = "Roomio profili için konumunu paylaşmalısın!")
	private Double longitude;

	private List<String> photoUrls; // Önce /api/upload/roomio-photo ile yüklenmiş URL'ler
}
