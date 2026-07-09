package com.ereniridere.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.Post;
import com.ereniridere.entity.enums.PostType;

public interface PostRepository extends JpaRepository<Post, Integer> {

	// 🚨 ÇÖZÜM 1: JOIN FETCH İLE TEK SORGUDA BÜTÜN İLİŞKİLERİ ÇEKİYORUZ 🚨
	@Query("SELECT p FROM Post p " + "JOIN FETCH p.author a " + "JOIN FETCH p.neighborhood n "
			+ "LEFT JOIN FETCH a.merchantProfile m "
			+ "WHERE n.id = :neighborhoodId AND p.isActive = true AND a.id != :userId "
			+ "AND (:type IS NULL OR p.type = :type) "
			+ "ORDER BY CASE WHEN p.type = com.ereniridere.entity.enums.PostType.SPONSORED THEN 1 ELSE 2 END, p.createdAt DESC")
	Page<Post> getNeighborhoodFeedExcludingMe(@Param("neighborhoodId") Integer neighborhoodId,
			@Param("userId") Integer userId, @Param("type") PostType type, Pageable pageable);

	// 2. KENDİ BİREYSEL POSTLARIM (Buna da JOIN FETCH ekledik hızlansın diye)
	@Query("SELECT p FROM Post p " + "JOIN FETCH p.author a " + "JOIN FETCH p.neighborhood n "
			+ "LEFT JOIN FETCH a.merchantProfile m "
			+ "WHERE a.id = :userId AND p.isActive = true AND p.type != com.ereniridere.entity.enums.PostType.SPONSORED "
			+ "ORDER BY p.createdAt DESC")
	Page<Post> getMyStandardPosts(@Param("userId") Integer userId, Pageable pageable);

	// 3. KENDİ ESNAF POSTLARIM (Buna da JOIN FETCH ekledik)
	@Query("SELECT p FROM Post p " + "JOIN FETCH p.author a " + "JOIN FETCH p.neighborhood n "
			+ "LEFT JOIN FETCH a.merchantProfile m "
			+ "WHERE a.id = :userId AND p.isActive = true AND p.type = com.ereniridere.entity.enums.PostType.SPONSORED "
			+ "ORDER BY p.createdAt DESC")
	Page<Post> getMySponsoredPosts(@Param("userId") Integer userId, Pageable pageable);

	// 🚨 SPONSORED YAKINLIK SORGUSU — ADIM 1 (iki-adımlı desen) 🚨
	// Esnafın dükkan konumu (merchant_profiles.geo_location) verilen noktaya 'radius'
	// METRE içinde olan, ONAYLI esnaflara ait, aktif SPONSORED postların ID sayfasını
	// döndürür. Kendi postlarımız (userId) hariç. En yakın esnaf en üstte (KNN <->).
	// geography cast = metre; (geo_location::geography) GiST index'i kullanılır.
	@Query(value = "SELECT p.id FROM posts p "
			+ "JOIN merchant_profiles m ON m.user_id = p.user_id "
			+ "WHERE p.is_active = true AND p.type = 'SPONSORED' "
			+ "AND m.is_verified = true AND p.user_id <> :userId "
			+ "AND m.geo_location IS NOT NULL "
			+ "AND ST_DWithin(m.geo_location::geography, "
			+ "    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius) "
			+ "ORDER BY m.geo_location::geography <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography",
			countQuery = "SELECT count(*) FROM posts p "
					+ "JOIN merchant_profiles m ON m.user_id = p.user_id "
					+ "WHERE p.is_active = true AND p.type = 'SPONSORED' "
					+ "AND m.is_verified = true AND p.user_id <> :userId "
					+ "AND m.geo_location IS NOT NULL "
					+ "AND ST_DWithin(m.geo_location::geography, "
					+ "    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)", nativeQuery = true)
	Page<Integer> findSponsoredNearbyPostIds(@Param("userId") Integer userId, @Param("lat") double lat,
			@Param("lng") double lng, @Param("radius") int radius, Pageable pageable);

	// 🚨 SPONSORED YAKINLIK SORGUSU — ADIM 2 (hidrasyon) 🚨
	// Mevcut feed'deki JOIN FETCH desenini koruyarak ID'lerden tek sorguda çeker (N+1 yok).
	// Sıralama servis katmanında ID listesine göre yeniden uygulanır.
	@Query("SELECT p FROM Post p JOIN FETCH p.author a JOIN FETCH p.neighborhood "
			+ "LEFT JOIN FETCH a.merchantProfile WHERE p.id IN :ids")
	List<Post> findAllByIdInWithFetch(@Param("ids") List<Integer> ids);

	// Hesap silme: kullanıcının yazdığı tüm postları sil.
	// (Önce bu postlara ait yorum/beğeniler temizlenmiş olmalı — FK kısıtı.)
	@Modifying
	@Transactional
	@Query("DELETE FROM Post p WHERE p.author.id = :userId")
	void deleteAllByAuthorId(@Param("userId") Integer userId);
}