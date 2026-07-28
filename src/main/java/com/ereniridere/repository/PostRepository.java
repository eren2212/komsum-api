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

	// 🚨 KEYSET (CURSOR) FEED: JOIN FETCH ile tek sorguda bütün ilişkiler (N+1 yok) 🚨
	// Offset yerine sıralama anahtarından (createdAt, id) devam eder: araya kaç yeni
	// post girerse girsin duplicate/atlama olmaz. Sıralama SAF kronolojik olmalı ki
	// cursor karşılaştırması ORDER BY ile birebir örtüşsün (SPONSORED float YOK).
	// PART 3: Kendi postların da akışta görünür (dışlama YOK) — paylaşınca en tepede.
	//
	// İlk sayfa (cursor yok) ve sonraki sayfalar (cursor var) BİLEREK iki ayrı sorguya
	// bölündü. Tek sorguda "(:cursorTime IS NULL OR p.createdAt < :cursorTime ...)"
	// yazılırsa PostgreSQL bu TIMESTAMP parametresinin tipini çıkaramıyor ve
	// "SQLState 42P18: could not determine data type of parameter" hatası atıyor
	// (enum p.type için bu sorun yok çünkü onun tipi entity metadata'sından biliniyor).
	// Bu sayede cursorTime her zaman gerçek bir değer olarak bağlanıyor, asla NULL değil.
	@Query("SELECT p FROM Post p " + "JOIN FETCH p.author a " + "JOIN FETCH p.neighborhood n "
			+ "LEFT JOIN FETCH a.merchantProfile m "
			+ "WHERE n.id = :neighborhoodId AND p.isActive = true "
			+ "AND (:type IS NULL OR p.type = :type) "
			+ "ORDER BY p.createdAt DESC, p.id DESC")
	List<Post> getNeighborhoodFeedFirstPage(@Param("neighborhoodId") Integer neighborhoodId,
			@Param("type") PostType type, Pageable pageable);

	@Query("SELECT p FROM Post p " + "JOIN FETCH p.author a " + "JOIN FETCH p.neighborhood n "
			+ "LEFT JOIN FETCH a.merchantProfile m "
			+ "WHERE n.id = :neighborhoodId AND p.isActive = true "
			+ "AND (:type IS NULL OR p.type = :type) "
			+ "AND (p.createdAt < :cursorTime "
			+ "     OR (p.createdAt = :cursorTime AND p.id < :cursorId)) "
			+ "ORDER BY p.createdAt DESC, p.id DESC")
	List<Post> getNeighborhoodFeedAfterCursor(@Param("neighborhoodId") Integer neighborhoodId,
			@Param("type") PostType type, @Param("cursorTime") java.time.LocalDateTime cursorTime,
			@Param("cursorId") Integer cursorId, Pageable pageable);

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

	// 🚨 PART 2: "Kaç yeni post var" — mahalle scope'unda (aktif, kendi postların hariç)
	// id'si verilen high-water mark'tan büyük olanların sayısı. id monoton arttığı için
	// createdAt yerine id ile saymak yeterli ve daha ucuz.
	@Query("SELECT COUNT(p) FROM Post p WHERE p.neighborhood.id = :neighborhoodId "
			+ "AND p.isActive = true AND p.author.id <> :userId AND p.id > :sinceId")
	long countNewPostsSince(@Param("neighborhoodId") Integer neighborhoodId, @Param("userId") Integer userId,
			@Param("sinceId") Integer sinceId);

	// Taban çizgisi kurmak için: scope içindeki en büyük (en yeni) post id'si. Post yoksa null.
	@Query("SELECT MAX(p.id) FROM Post p WHERE p.neighborhood.id = :neighborhoodId "
			+ "AND p.isActive = true AND p.author.id <> :userId")
	Integer findMaxPostIdInScope(@Param("neighborhoodId") Integer neighborhoodId, @Param("userId") Integer userId);

	// Hesap silme: kullanıcının yazdığı tüm postları sil.
	// (Önce bu postlara ait yorum/beğeniler temizlenmiş olmalı — FK kısıtı.)
	@Modifying
	@Transactional
	@Query("DELETE FROM Post p WHERE p.author.id = :userId")
	void deleteAllByAuthorId(@Param("userId") Integer userId);
}