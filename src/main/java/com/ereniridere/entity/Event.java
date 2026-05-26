package com.ereniridere.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Formula;
import org.locationtech.jts.geom.Point;

import com.ereniridere.entity.enums.EventCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(length = 500)
	private String description;

	@Column(name = "image_url")
	private String imageUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventCategory category;

	@Column(name = "event_date", nullable = false)
	private LocalDateTime eventDate;

	// Etkinliğin yapılacağı yer (Örn: "Japon Parkı", "Bosna Hersek Halı Saha")
	// DİKKAT: Bu mekânın METİN adı; aşağıdaki geoLocation ise harita koordinatı.
	@Column(nullable = false)
	private String location;

	// PostGIS spatial konum (SRID 4326). Eski Double latitude/longitude alanlarının
	// yerini aldı. İsim çakışmasını önlemek için 'location' (mekân adı) ile değil
	// 'geoLocation' adıyla tutuluyor. lat/lng dönüşümleri GeoUtils üzerinden yapılır.
	@Column(name = "geo_location", columnDefinition = "geometry(Point, 4326)")
	private Point geoLocation;

	// Eğer ücretliyse fiyatı, null ise "Ücretsiz" kabul edebiliriz
	@Column(name = "price_text")
	private String priceText;

	@ManyToOne
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	// Etkinlik hangi mahallede yapılıyor? (Sadece o mahalledekiler görsün diye)
	@ManyToOne
	@JoinColumn(name = "neighborhood_id", nullable = false)
	private Neighborhood neighborhood;

	@Column(name = "is_active")
	private boolean isActive = true;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	// 🚨 SİHİRLİ SAYAÇ: Kaç kişi "Katılıyorum" dedi?
	@Formula("(SELECT COUNT(*) FROM event_participants ep WHERE ep.event_id = id)")
	private Integer participantCount;
}