package com.ereniridere.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
	Page<Comment> findByPostIdAndIsActiveTrueOrderByCreatedAtAsc(Integer postId,
			org.springframework.data.domain.Pageable pageable);

	// Hesap silme: kullanıcının yazdığı yorumlar VE kullanıcının postlarına gelen
	// tüm yorumlar (FK kısıtı için postlar silinmeden önce temizlenir).
	@Modifying
	@Transactional
	@Query("DELETE FROM Comment c WHERE c.author.id = :userId "
			+ "OR c.post.id IN (SELECT p.id FROM Post p WHERE p.author.id = :userId)")
	void deleteAllByAuthorIdOrAuthoredPosts(@Param("userId") Integer userId);
}
