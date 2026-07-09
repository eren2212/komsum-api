package com.ereniridere.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.Event;

public interface EventRepository extends JpaRepository<Event, Integer> {

	// 🚨 İŞTE O MÜKEMMEL İLÇE SORGUSU 🚨
	// "Bana, bağlı olduğu mahallenin ilçesi (district) benim gönderdiğim ilçe ile
	// aynı olan aktif etkinlikleri getir. En yakın tarihli olan en üstte olsun!"
	// NOT: /api/events/nearby konum göndermediğinde geriye-uyumlu fallback olarak kalır.
	@Query("SELECT e FROM Event e WHERE e.neighborhood.district = :district AND e.isActive = true ORDER BY e.eventDate ASC")
	Page<Event> findEventsByDistrict(@Param("district") String district, Pageable pageable);

	// BENİM OLUŞTURDUĞUM ETKİNLİKLER (En yeniler en üstte)
	@Query("SELECT e FROM Event e WHERE e.author.id = :userId AND e.isActive = true ORDER BY e.createdAt DESC")
	Page<Event> findByAuthorId(@Param("userId") Integer userId, Pageable pageable);

	// 🚨 SPATIAL YAKINLIK SORGUSU — ADIM 1 (iki-adımlı desen) 🚨
	// Verilen konuma 'radius' METRE içindeki aktif etkinliklerin ID sayfasını döndürür.
	// geography cast metre bazlı ölçüm sağlar (geometry+4326 mesafeyi DERECE sayardı).
	// (geo_location::geography) üzerindeki GiST functional index bu sorguyu hızlandırır.
	// En yakın tarihli etkinlik en üstte. Asıl hidrasyon findAllByIdInWithFetch ile yapılır.
	@Query(value = "SELECT e.id FROM events e "
			+ "WHERE e.is_active = true AND e.geo_location IS NOT NULL "
			+ "AND ST_DWithin(e.geo_location::geography, "
			+ "    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius) "
			+ "ORDER BY e.event_date ASC", countQuery = "SELECT count(*) FROM events e "
					+ "WHERE e.is_active = true AND e.geo_location IS NOT NULL "
					+ "AND ST_DWithin(e.geo_location::geography, "
					+ "    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)", nativeQuery = true)
	Page<Integer> findNearbyEventIds(@Param("lat") double lat, @Param("lng") double lng,
			@Param("radius") int radius, Pageable pageable);

	// 🚨 SPATIAL YAKINLIK SORGUSU — ADIM 2 (hidrasyon) 🚨
	// ID'lerden author + neighborhood'u TEK sorguda JOIN FETCH ile çeker (N+1 yok).
	// Sıralama servis katmanında ID listesine göre yeniden uygulanır.
	@Query("SELECT e FROM Event e JOIN FETCH e.author JOIN FETCH e.neighborhood WHERE e.id IN :ids")
	List<Event> findAllByIdInWithFetch(@Param("ids") List<Integer> ids);

	// Hesap silme: kullanıcının oluşturduğu tüm etkinlikleri sil.
	// (Önce katılım/bookmark kayıtları temizlenmiş olmalı — FK kısıtı.)
	@Modifying
	@Transactional
	@Query("DELETE FROM Event e WHERE e.author.id = :userId")
	void deleteAllByAuthorId(@Param("userId") Integer userId);
}