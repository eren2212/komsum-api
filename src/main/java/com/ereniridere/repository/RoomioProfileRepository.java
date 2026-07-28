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

import com.ereniridere.entity.RoomioProfile;

@Repository
public interface RoomioProfileRepository extends JpaRepository<RoomioProfile, Integer> {

	// Kullanıcının zaten bir Roomio profili var mı kontrolü için
	Optional<RoomioProfile> findByUserId(Integer userId);

	// 🚨 SPATIAL YAKINLIK SORGUSU — ADIM 1 (iki-adımlı desen, ServiceProviderProfileRepository ile aynı) 🚨
	// Kendini, aktif olmayanları ve zaten swipe ettiklerini (yön farketmeksizin
	// ben swipe etmişsem) hariç tutar. En yakın aday en üstte (KNN <-> operatörü).
	// Asıl hidrasyon findAllByIdInWithFetch ile yapılır (N+1 yok).
	@Query(value = "SELECT r.id FROM roomio_profiles r "
			+ "WHERE r.is_active = true AND r.geo_location IS NOT NULL AND r.user_id != :selfUserId "
			+ "AND r.user_id NOT IN (SELECT s.swiped_id FROM roomio_swipes s WHERE s.swiper_id = :selfUserId) "
			+ "AND ST_DWithin(r.geo_location::geography, "
			+ "    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius) "
			+ "ORDER BY r.geo_location::geography <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography",
			countQuery = "SELECT count(*) FROM roomio_profiles r "
					+ "WHERE r.is_active = true AND r.geo_location IS NOT NULL AND r.user_id != :selfUserId "
					+ "AND r.user_id NOT IN (SELECT s.swiped_id FROM roomio_swipes s WHERE s.swiper_id = :selfUserId) "
					+ "AND ST_DWithin(r.geo_location::geography, "
					+ "    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)", nativeQuery = true)
	Page<Integer> findNearbyCandidateIds(@Param("selfUserId") Integer selfUserId, @Param("lat") double lat,
			@Param("lng") double lng, @Param("radius") int radius, Pageable pageable);

	// 🚨 SPATIAL YAKINLIK SORGUSU — ADIM 2 (hidrasyon) 🚨
	// ID'lerden user + photos'u TEK sorguda JOIN FETCH ile çeker (N+1 yok).
	// Sıralama servis katmanında ID listesine göre yeniden uygulanır.
	@Query("SELECT DISTINCT r FROM RoomioProfile r JOIN FETCH r.user JOIN FETCH r.photos WHERE r.id IN :ids")
	List<RoomioProfile> findAllByIdInWithFetch(@Param("ids") List<Integer> ids);

	// Hesap silme: fotoğraflar FK ile roomio_profiles'a bağlı — profil silinmeden
	// ÖNCE bunlar silinmeli (orphanRemoval bulk delete'te devreye girmez).
	@Modifying
	@Transactional
	@Query(value = "DELETE FROM roomio_profile_photos WHERE roomio_profile_id IN "
			+ "(SELECT id FROM roomio_profiles WHERE user_id = :userId)", nativeQuery = true)
	void deletePhotosByUserId(@Param("userId") Integer userId);

	// Hesap silme: kullanıcının Roomio profilini sil.
	@Modifying
	@Transactional
	@Query("DELETE FROM RoomioProfile r WHERE r.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);
}
