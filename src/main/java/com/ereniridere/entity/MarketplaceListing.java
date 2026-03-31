package com.ereniridere.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ereniridere.entity.enums.ListingStatus;
import com.ereniridere.entity.enums.ListingType;

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
@Table(name = "marketplace_listings")
public class MarketplaceListing {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	// Tasarımdaki "Ürün Başlığı"
	@Column(nullable = false)
	private String title;

	// Supabase'den gelecek fotoğraf URL'si (Mobildeki Fotoğraf Ekle kısmı)
	@Column(nullable = false)
	private String imageUrl;

	// Tasarımdaki "İlan Türü" (Satılık veya Takas)
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ListingType type;

	// Tasarımdaki "Fiyat".
	// DİKKAT: Double yerine BigDecimal kullanıyoruz çünkü para hesaplamalarında
	// (ileride komisyon vs. olursa) BigDecimal hatasızdır!
	// Takas/Hediye ise bu alan null kalabilir.
	private BigDecimal price;

	// Tasarımdaki "Kategori" (Şimdilik String bırakıyoruz, MVP sonrası ayrı tablo
	// yapılabilir)
	@Column(nullable = false)
	private String category;

	// İlanın yayında mı yoksa satıldı mı durumu
	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(nullable = false)
	private ListingStatus status = ListingStatus.ACTIVE;

	// SENIOR DOKUNUŞU 1: İlan KİMİN?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// SENIOR DOKUNUŞU 2: İlan HANGİ MAHALLEDE?
	// (Pazar yerinde sadece kendi mahallesindeki ürünleri görsün diye)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "neighborhood_id", nullable = false)
	private Neighborhood neighborhood;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
}
