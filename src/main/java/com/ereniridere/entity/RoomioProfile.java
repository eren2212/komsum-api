package com.ereniridere.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import com.ereniridere.entity.enums.RoomioGender;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roomio_profiles")
public class RoomioProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	// Roomio profili KİMİN? (Sadece 1 tane olabilir, Komşum kullanıcısıyla 1-1 eşleşir)
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(columnDefinition = "TEXT")
	private String bio;

	@Column(nullable = false)
	private Integer age;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RoomioGender gender;

	// PostGIS spatial konum (SRID 4326). Aday akışı yarıçap (ST_DWithin) bazlı
	// çalışır — mahalle sadece gösterim amaçlı, filtrede KULLANILMAZ.
	// lat/lng dönüşümleri GeoUtils üzerinden yapılır.
	@Column(name = "geo_location", columnDefinition = "geometry(Point, 4326)")
	private Point geoLocation;

	// Gösterim amaçlı — profil oluşturulurken User'dan kopyalanır.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "neighborhood_id")
	private Neighborhood neighborhood;

	// Kullanıcı profilini geçici olarak gizleyebilsin diye (aday akışından çıkar).
	@Builder.Default
	@Column(name = "is_active")
	private boolean isActive = true;

	@OneToMany(mappedBy = "roomioProfile", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderColumn(name = "sort_order")
	@Builder.Default
	private List<RoomioProfilePhoto> photos = new ArrayList<>();

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;
}
