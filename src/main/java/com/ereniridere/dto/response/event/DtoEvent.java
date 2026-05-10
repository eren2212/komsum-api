package com.ereniridere.dto.response.event;

import java.time.LocalDateTime;

import com.ereniridere.entity.enums.EventCategory;

import lombok.Data;

@Data
public class DtoEvent {
	private Integer id;
	private String title;
	private String description;
	private String imageUrl;
	private EventCategory category;
	private LocalDateTime eventDate;
	private String location;
	private Double latitude;
	private Double longitude;
	private String priceText;

	private Integer authorId;
	private String authorFirstName;
	private String authorLastName;
	private String neighborhoodName; // Örn: "Bosna Hersek Mahallesi"

	// 🚨 Uİ İÇİN SİHİRLİ ALANLAR 🚨
	private Integer participantCount; // "32 Kişi Katılıyor"
	private boolean joinedByMe; // Ben bu etkinliğe katıldım mı? (Buton rengi için)
	private boolean bookmarkedByMe; // Ben bunu kaydettim mi? (Sağ üstteki ikon için)
}