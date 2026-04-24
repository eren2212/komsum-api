package com.ereniridere.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ereniridere.entity.Post;

public interface PostRepository extends JpaRepository<Post, Integer> {

	// 1. ANA AKIŞ: Mahalledeki aktif postlar, AMA BENİM ATTIKLARIM HARİÇ!
	// (Sponsorlular en üstte mantığı korundu)
	@Query("SELECT p FROM Post p WHERE p.neighborhood.id = :neighborhoodId AND p.isActive = true AND p.author.id != :userId "
			+ "ORDER BY CASE WHEN p.type = com.ereniridere.entity.enums.PostType.SPONSORED THEN 1 ELSE 2 END, p.createdAt DESC")
	Page<Post> getNeighborhoodFeedExcludingMe(@Param("neighborhoodId") Integer neighborhoodId,
			@Param("userId") Integer userId, Pageable pageable);

	// 2. KENDİ BİREYSEL POSTLARIM: Sadece bana ait ve Sponsorlu OLMAYANLAR (Normal
	// ve Yardım)
	@Query("SELECT p FROM Post p WHERE p.author.id = :userId AND p.isActive = true AND p.type != com.ereniridere.entity.enums.PostType.SPONSORED ORDER BY p.createdAt DESC")
	Page<Post> getMyStandardPosts(@Param("userId") Integer userId, Pageable pageable);

	// 3. KENDİ ESNAF POSTLARIM: Sadece bana ait ve SPONSORLU olanlar
	@Query("SELECT p FROM Post p WHERE p.author.id = :userId AND p.isActive = true AND p.type = com.ereniridere.entity.enums.PostType.SPONSORED ORDER BY p.createdAt DESC")
	Page<Post> getMySponsoredPosts(@Param("userId") Integer userId, Pageable pageable);
}