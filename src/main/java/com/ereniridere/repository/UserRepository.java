package com.ereniridere.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.User;

@Repository
// JpaRepository<Hangi Tablo, O Tablonun ID Tipi>
public interface UserRepository extends JpaRepository<User, Integer> {

	// Spring Data JPA'nın sihri! Tek satır SQL yazmadan isimlendirme kuralıyla
	// e-postadan kullanıcı bulan metod.
	Optional<User> findByEmail(String email);

	// Bildirim için: aynı ilçedeki, hedef kullanıcı hariç tüm üyeler.
	// Sadece FCM token'ı olanları çekmek için ek filtre koymuyoruz —
	// bildirim DB'ye de yazılacağı için token'sız kullanıcılar da gerekir.
	@Query("SELECT u FROM User u JOIN u.neighborhood n " +
			"WHERE n.district = :district AND u.id <> :excludeUserId")
	List<User> findByDistrictExcludingUser(@Param("district") String district,
			@Param("excludeUserId") Integer excludeUserId);

	// Geçersiz FCM token'ları temizlemek için.
	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.fcmToken = NULL WHERE u.fcmToken = :token")
	void clearFcmToken(@Param("token") String token);

	// PART 2: "En son görülen" işaretini yalnızca İLERİ taşı (geri gitmesin).
	// Mevcut değer null ise veya verilenden küçükse günceller.
	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.lastSeenPostId = :postId "
			+ "WHERE u.id = :userId AND (u.lastSeenPostId IS NULL OR u.lastSeenPostId < :postId)")
	int advanceLastSeenPostId(@Param("userId") Integer userId, @Param("postId") Integer postId);

}