package com.ereniridere.dto.response.roomio;

import java.util.List;

import com.ereniridere.entity.enums.RoomioGender;

import lombok.Data;

@Data
public class DtoRoomioProfile {
	private Integer id;

	private Integer userId;

	private String firstName;

	private String lastName;

	private String bio;

	private Integer age;

	private RoomioGender gender;

	private List<String> photoUrls;

	// Roomio profilinin harita konumu. Konum girilmemişse null.
	private Double latitude;

	private Double longitude;

	// Sadece gösterim amaçlı — eşleşme filtresinde kullanılmaz.
	private String neighborhoodName;

	private boolean isActive;
}
