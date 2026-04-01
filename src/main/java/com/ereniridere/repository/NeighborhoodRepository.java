package com.ereniridere.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ereniridere.entity.Neighborhood;

public interface NeighborhoodRepository extends JpaRepository<Neighborhood, Integer> {

	// 1. Sadece O Şehre Ait Benzersiz (DISTINCT) İlçeleri Alfabetik Getir (Örn:
	// Konya -> Meram, Selçuklu)
	@Query("SELECT DISTINCT n.district FROM Neighborhood n WHERE n.city = :city ORDER BY n.district ASC")
	List<String> findDistinctDistrictsByCity(@Param("city") String city);

	// 2. Seçilen İlçeye Ait Tüm Mahalleleri Alfabetik Getir (Dropdown'ın 2. adımı)
	List<Neighborhood> findByDistrictOrderByNameAsc(String district);
}