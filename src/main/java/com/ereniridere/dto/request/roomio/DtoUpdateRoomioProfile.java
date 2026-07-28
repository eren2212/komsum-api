package com.ereniridere.dto.request.roomio;

import java.util.List;

import com.ereniridere.entity.enums.RoomioGender;

import lombok.Data;

@Data
public class DtoUpdateRoomioProfile {
	// Güncelleme işleminde her alan opsiyonel — kullanıcı sadece tek bir şeyi
	// değiştirmek isteyebilir.
	private String bio;
	private Integer age;
	private RoomioGender gender;
	private List<String> photoUrls;

	// Konum güncellemesi: lat/lng ikisi birden geldiyse Point yenilenir.
	private Double latitude;
	private Double longitude;
}
