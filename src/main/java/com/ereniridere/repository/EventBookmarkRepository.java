package com.ereniridere.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ereniridere.entity.EventBookmark;

public interface EventBookmarkRepository extends JpaRepository<EventBookmark, Integer> {

	// Toggle (Kaydet/Çıkar) işlemi için var mı yok mu diye bulur
	Optional<EventBookmark> findByEventIdAndUserId(Integer eventId, Integer userId);

	// Ekranda sağ üstteki bookmark ikonunu dolu/boş yapmak için (true/false döner)
	boolean existsByEventIdAndUserId(Integer eventId, Integer userId);

	// PROFİL SEKMESİ İÇİN: Kullanıcının kaydettiği etkinlikleri sayfa sayfa getirir
	Page<EventBookmark> findByUserIdOrderBySavedAtDesc(Integer userId, Pageable pageable);

}