package com.ereniridere.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ereniridere.entity.Badge;

public interface BadgeRepository extends JpaRepository<Badge, Integer> {

	boolean existsByName(String name);

	// Puan eşiğine göre artan sırada — rozet ilerlemesini göstermek için.
	List<Badge> findAllByOrderByPointThresholdAsc();

	List<Badge> findByPointThresholdLessThanEqual(Integer points);
}
