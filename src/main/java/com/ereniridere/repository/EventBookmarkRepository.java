package com.ereniridere.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.EventBookmark;

public interface EventBookmarkRepository extends JpaRepository<EventBookmark, Integer> {

	// Toggle (Kaydet/Çıkar) işlemi için var mı yok mu diye bulur
	Optional<EventBookmark> findByEventIdAndUserId(Integer eventId, Integer userId);

	// Ekranda sağ üstteki bookmark ikonunu dolu/boş yapmak için (true/false döner)
	boolean existsByEventIdAndUserId(Integer eventId, Integer userId);

	// PROFİL SEKMESİ İÇİN: Kullanıcının kaydettiği etkinlikleri sayfa sayfa getirir
	Page<EventBookmark> findByUserIdOrderBySavedAtDesc(Integer userId, Pageable pageable);

	// Hesap silme: kullanıcının kayıtları VE sildiği etkinliklere ait tüm
	// bookmark kayıtları (FK kısıtı için etkinlikler silinmeden önce temizlenir).
	@Modifying
	@Transactional
	@Query("DELETE FROM EventBookmark eb WHERE eb.user.id = :userId "
			+ "OR eb.event.id IN (SELECT e.id FROM Event e WHERE e.author.id = :userId)")
	void deleteAllByUserIdOrAuthoredEvents(@Param("userId") Integer userId);

}
