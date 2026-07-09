package com.ereniridere.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.EventParticipant;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Integer> {

	// Toggle (Katıl/Ayrıl) işlemi için var mı yok mu diye bulur
	Optional<EventParticipant> findByEventIdAndUserId(Integer eventId, Integer userId);

	// Ekranda buton rengini ayarlamak için (true/false döner)
	boolean existsByEventIdAndUserId(Integer eventId, Integer userId);

	// PROFİL SEKMESİ İÇİN: Kullanıcının katıldığı etkinlikleri sayfa sayfa getirir
	Page<EventParticipant> findByUserIdOrderByJoinedAtDesc(Integer userId, Pageable pageable);

	// Hesap silme: kullanıcının katılımları VE sildiği etkinliklere ait tüm
	// katılım kayıtları (FK kısıtı için etkinlikler silinmeden önce temizlenir).
	@Modifying
	@Transactional
	@Query("DELETE FROM EventParticipant ep WHERE ep.user.id = :userId "
			+ "OR ep.event.id IN (SELECT e.id FROM Event e WHERE e.author.id = :userId)")
	void deleteAllByUserIdOrAuthoredEvents(@Param("userId") Integer userId);

}
