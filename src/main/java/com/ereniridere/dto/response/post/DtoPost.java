package com.ereniridere.dto.response.post;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoPost {
	private Integer id;
	private String content;
	private String imageUrl;
	private String type;

	// Yapan adamın bilgilerini dönüyoruz ki React Native'de "Eren - 5 dk önce"
	// yazabilesin
	private String authorFirstName;
	private String authorLastName;
	private Integer authorKarmaScore; // Adamın puanını da postun köşesinde gösterebiliriz!

	private String neighborhoodName;
	private LocalDateTime createdAt;
}