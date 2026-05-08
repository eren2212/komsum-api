package com.ereniridere.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ereniridere.entity.EventParticipant;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Integer> {

	// Toggle (Katıl/Ayrıl) işlemi için var mı yok mu diye bulur
	Optional<EventParticipant> findByEventIdAndUserId(Integer eventId, Integer userId);

	// Ekranda buton rengini ayarlamak için (true/false döner)
	boolean existsByEventIdAndUserId(Integer eventId, Integer userId);

	// PROFİL SEKMESİ İÇİN: Kullanıcının katıldığı etkinlikleri sayfa sayfa getirir
	Page<EventParticipant> findByUserIdOrderByJoinedAtDesc(Integer userId, Pageable pageable);

}