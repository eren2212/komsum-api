package com.ereniridere.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ereniridere.entity.MarketplaceListing;
import com.ereniridere.entity.enums.ListingStatus;

public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, Integer> {

	// 1. VİTRİN: Mahalledeki, ACTIVE olan ve BANA AİT OLMAYAN (UserIdNot) ilanlar!
	Page<MarketplaceListing> findByNeighborhoodIdAndStatusAndUserIdNotOrderByCreatedAtDesc(Integer neighborhoodId,
			ListingStatus status, Integer userId, // Dışlanacak olan adamın ID'si
			Pageable pageable);

	// 2. İLANLARIM: Sadece bana ait olan ilanlar (Durumu ACTIVE veya SOLD fark
	// etmez, hepsini kendi sayfasında görsün)
	Page<MarketplaceListing> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}