package com.ereniridere.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ereniridere.entity.Post;
import com.ereniridere.entity.enums.PostType;

public interface PostRepository extends JpaRepository<Post, Integer> {

	// 🚨 ÇÖZÜM 1: JOIN FETCH İLE TEK SORGUDA BÜTÜN İLİŞKİLERİ ÇEKİYORUZ 🚨
	@Query("SELECT p FROM Post p " + "JOIN FETCH p.author a " + "JOIN FETCH p.neighborhood n "
			+ "LEFT JOIN FETCH a.merchantProfile m "
			+ "WHERE n.id = :neighborhoodId AND p.isActive = true AND a.id != :userId "
			+ "AND (:type IS NULL OR p.type = :type) "
			+ "ORDER BY CASE WHEN p.type = com.ereniridere.entity.enums.PostType.SPONSORED THEN 1 ELSE 2 END, p.createdAt DESC")
	Page<Post> getNeighborhoodFeedExcludingMe(@Param("neighborhoodId") Integer neighborhoodId,
			@Param("userId") Integer userId, @Param("type") PostType type, Pageable pageable);

	// 2. KENDİ BİREYSEL POSTLARIM (Buna da JOIN FETCH ekledik hızlansın diye)
	@Query("SELECT p FROM Post p " + "JOIN FETCH p.author a " + "JOIN FETCH p.neighborhood n "
			+ "LEFT JOIN FETCH a.merchantProfile m "
			+ "WHERE a.id = :userId AND p.isActive = true AND p.type != com.ereniridere.entity.enums.PostType.SPONSORED "
			+ "ORDER BY p.createdAt DESC")
	Page<Post> getMyStandardPosts(@Param("userId") Integer userId, Pageable pageable);

	// 3. KENDİ ESNAF POSTLARIM (Buna da JOIN FETCH ekledik)
	@Query("SELECT p FROM Post p " + "JOIN FETCH p.author a " + "JOIN FETCH p.neighborhood n "
			+ "LEFT JOIN FETCH a.merchantProfile m "
			+ "WHERE a.id = :userId AND p.isActive = true AND p.type = com.ereniridere.entity.enums.PostType.SPONSORED "
			+ "ORDER BY p.createdAt DESC")
	Page<Post> getMySponsoredPosts(@Param("userId") Integer userId, Pageable pageable);
}