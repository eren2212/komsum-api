package com.ereniridere.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ereniridere.entity.Post;

public interface PostRepository extends JpaRepository<Post, Integer> {

	@Query("SELECT p FROM Post p WHERE p.neighborhood.id = :neighborhoodId AND p.isActive = true " + // <-- BURAYA
																										// DİKKAT
			"ORDER BY CASE WHEN p.type = com.ereniridere.entity.enums.PostType.SPONSORED THEN 1 ELSE 2 END, p.createdAt DESC")
	Page<Post> getNeighborhoodFeed(@Param("neighborhoodId") Integer neighborhoodId, Pageable pageable);

	// Ekranda sadece silinmemiş (isActive=true) ve kendi attığı postları, en yenisi
	// en üstte olacak şekilde getirir.
	Page<Post> findByAuthorIdAndIsActiveTrueOrderByCreatedAtDesc(Integer authorId, Pageable pageable);
}
