package com.ereniridere.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ereniridere.entity.Event;

public interface EventRepository extends JpaRepository<Event, Integer> {

	// 🚨 İŞTE O MÜKEMMEL İLÇE SORGUSU 🚨
	// "Bana, bağlı olduğu mahallenin ilçesi (district) benim gönderdiğim ilçe ile
	// aynı olan aktif etkinlikleri getir. En yakın tarihli olan en üstte olsun!"
	@Query("SELECT e FROM Event e WHERE e.neighborhood.district = :district AND e.isActive = true ORDER BY e.eventDate ASC")
	Page<Event> findEventsByDistrict(@Param("district") String district, Pageable pageable);

	// BENİM OLUŞTURDUĞUM ETKİNLİKLER (En yeniler en üstte)
	@Query("SELECT e FROM Event e WHERE e.author.id = :userId AND e.isActive = true ORDER BY e.createdAt DESC")
	Page<Event> findByAuthorId(@Param("userId") Integer userId, Pageable pageable);
}