package com.ereniridere.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "merchant_profiles")
public class MerchantProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false)
	private String shopName; // Dükkan Adı (Örn: Kasap Ahmet)

	@Column(nullable = false)
	private String category; // Kategori (Örn: Gıda, Tesisat, Temizlik) - İleride bunu da Enum yapabiliriz
								// ama MVP için String harikadır.

	@Column(nullable = false)
	private String phone; // Esnafa ulaşılacak numara

	@Column(nullable = false)
	private String address; // Dükkanın fiziksel adresi

	@Column(columnDefinition = "TEXT")
	private String description; // "1990'dan beri hizmetinizdeyiz..."

	// SENIOR DOKUNUŞU 1: Güvenlik!
	// Adam kayıt olur olmaz esnaf sayfasında çıkmasın. Biz (Admin) onaylayınca true
	// olsun.
	@Builder.Default
	private boolean isVerified = false;

	// SENIOR DOKUNUŞU 2: Bu dükkan KİMİN? (Sadece 1 tane dükkanı olabilir)
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	// SENIOR DOKUNUŞU 3: Bu dükkan HANGİ MAHALLEDE?
	// (Rehberi çizerken "Sadece bu mahalledeki esnafları getir" demek için şart)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "neighborhood_id", nullable = false)
	private Neighborhood neighborhood;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
}