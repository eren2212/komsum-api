package com.ereniridere.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import com.ereniridere.entity.enums.ServiceCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * "Gerekli Kişiler / Ustalar" profili. Esnaf (MerchantProfile) ile birebir paralel
 * ama ayrı tablo — bir kullanıcı HEM esnaf HEM usta olabilir, esnafın SPONSORED/radius
 * mantığına dokunulmaz.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_provider_profiles")
public class ServiceProviderProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false)
	private String title; // Görünen başlık (Örn: "Matematik Öğretmeni", "Tesisatçı Ahmet")

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ServiceCategory category; // Meslek kategorisi

	@Column(nullable = false)
	private String phone; // Ulaşılacak numara

	@Column(columnDefinition = "TEXT")
	private String description; // Tanıtım / hizmet açıklaması

	private Integer experienceYears; // Deneyim yılı (opsiyonel)

	private String priceInfo; // Ücret bilgisi — serbest metin (Örn: "500₺/saat", "Pazarlığa açık")

	@Builder.Default
	private boolean available = true; // Müsaitlik durumu — usta "iş alıyorum / almıyorum" diye açıp kapatır

	@Column(nullable = false)
	private String address; // Açık adres (metin)

	// PostGIS spatial konum (SRID 4326). Haritada pin göstermek ve ileride "yakındaki
	// ustalar" sorgusu için. lat/lng dönüşümleri GeoUtils üzerinden yapılır.
	@Column(name = "geo_location", columnDefinition = "geometry(Point, 4326)")
	private Point geoLocation;

	// Admin onayı — rehberde yalnızca onaylananlar (isVerified=true) görünür.
	@Builder.Default
	private boolean isVerified = false;

	private String profileImageUrl;

	// Bu profil KİMİN? (kullanıcı başına yalnızca 1 usta profili)
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	// Bu usta HANGİ MAHALLEDE? (rehberi mahalle bazlı çizmek için şart)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "neighborhood_id", nullable = false)
	private Neighborhood neighborhood;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
}
