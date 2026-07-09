package com.ereniridere.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.ServiceProviderProfile;
import com.ereniridere.entity.enums.ServiceCategory;

@Repository
public interface ServiceProviderProfileRepository extends JpaRepository<ServiceProviderProfile, Integer> {

	// 1. Kullanıcının zaten bir usta profili var mı kontrolü için
	Optional<ServiceProviderProfile> findByUserId(Integer userId);

	// 2. "Gerekli Kişiler" rehberi: o mahalledeki ve ONAYLANMIŞ ustalar
	List<ServiceProviderProfile> findByNeighborhoodIdAndIsVerifiedTrue(Integer neighborhoodId);

	// 3. Aynı rehberin kategori filtreli hâli (mobilde çip seçimi)
	List<ServiceProviderProfile> findByNeighborhoodIdAndCategoryAndIsVerifiedTrue(Integer neighborhoodId,
			ServiceCategory category);

	// 🚨 SPATIAL YAKINLIK SORGUSU — ADIM 1 (iki-adımlı desen) 🚨
	// Verilen konuma 'radius' METRE içindeki ONAYLI ustaların ID sayfasını döndürür.
	// geography cast metre bazlı ölçüm sağlar; (geo_location::geography) GiST functional
	// index bu sorguyu hızlandırır. En yakın usta en üstte (KNN <-> operatörü).
	// Asıl hidrasyon findAllByIdInWithFetch ile yapılır (N+1 yok).
	@Query(value = "SELECT s.id FROM service_provider_profiles s "
			+ "WHERE s.is_verified = true AND s.geo_location IS NOT NULL "
			+ "AND ST_DWithin(s.geo_location::geography, "
			+ "    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius) "
			+ "ORDER BY s.geo_location::geography <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography",
			countQuery = "SELECT count(*) FROM service_provider_profiles s "
					+ "WHERE s.is_verified = true AND s.geo_location IS NOT NULL "
					+ "AND ST_DWithin(s.geo_location::geography, "
					+ "    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)", nativeQuery = true)
	Page<Integer> findNearbyServiceProviderIds(@Param("lat") double lat, @Param("lng") double lng,
			@Param("radius") int radius, Pageable pageable);

	// 🚨 SPATIAL YAKINLIK SORGUSU — ADIM 1 (kategori filtreli) 🚨
	// category, enum'un name()'i olarak (String) bağlanır; kolon @Enumerated(STRING).
	@Query(value = "SELECT s.id FROM service_provider_profiles s "
			+ "WHERE s.is_verified = true AND s.geo_location IS NOT NULL "
			+ "AND s.category = :category "
			+ "AND ST_DWithin(s.geo_location::geography, "
			+ "    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius) "
			+ "ORDER BY s.geo_location::geography <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography",
			countQuery = "SELECT count(*) FROM service_provider_profiles s "
					+ "WHERE s.is_verified = true AND s.geo_location IS NOT NULL "
					+ "AND s.category = :category "
					+ "AND ST_DWithin(s.geo_location::geography, "
					+ "    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)", nativeQuery = true)
	Page<Integer> findNearbyServiceProviderIdsByCategory(@Param("lat") double lat, @Param("lng") double lng,
			@Param("radius") int radius, @Param("category") String category, Pageable pageable);

	// 🚨 SPATIAL YAKINLIK SORGUSU — ADIM 2 (hidrasyon) 🚨
	// ID'lerden user + neighborhood'u TEK sorguda JOIN FETCH ile çeker (N+1 yok).
	// Sıralama servis katmanında ID listesine göre yeniden uygulanır.
	@Query("SELECT s FROM ServiceProviderProfile s JOIN FETCH s.user JOIN FETCH s.neighborhood WHERE s.id IN :ids")
	List<ServiceProviderProfile> findAllByIdInWithFetch(@Param("ids") List<Integer> ids);

	// Hesap silme: kullanıcının usta profilini sil.
	@Modifying
	@Transactional
	@Query("DELETE FROM ServiceProviderProfile s WHERE s.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

}
