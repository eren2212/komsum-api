package com.ereniridere.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.MarketplaceListing;
import com.ereniridere.entity.enums.ListingStatus;

public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, Integer> {

	// 1. VİTRİN: Tek sorguda ilanı, satıcıyı ve mahalleyi çeker (HIZ KANATLANIYOR)
	@Query("SELECT m FROM MarketplaceListing m " + "JOIN FETCH m.user u " + "JOIN FETCH m.neighborhood n "
			+ "WHERE n.id = :neighborhoodId AND m.status = :status AND u.id != :userId " + "ORDER BY m.createdAt DESC")
	Page<MarketplaceListing> getNeighborhoodVitrin(@Param("neighborhoodId") Integer neighborhoodId,
			@Param("status") ListingStatus status, @Param("userId") Integer userId, Pageable pageable);

	// 2. İLANLARIM: Kendi ilanlarımı çekerken de N+1 sorununu eziyoruz
	@Query("SELECT m FROM MarketplaceListing m " + "JOIN FETCH m.user u " + "JOIN FETCH m.neighborhood n "
			+ "WHERE u.id = :userId " + "ORDER BY m.createdAt DESC")
	Page<MarketplaceListing> getMyListings(@Param("userId") Integer userId, Pageable pageable);

	// Hesap silme: kullanıcının tüm ilanlarını sil.
	@Modifying
	@Transactional
	@Query("DELETE FROM MarketplaceListing m WHERE m.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);
}