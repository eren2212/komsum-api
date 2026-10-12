package com.ereniridere.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
	// Sadece top-level yorumlar (cevaplar hariç). Engelleme iki yönlü: görüntüleyenle
	// arasında engel olan kullanıcıların yorumları listelenmez.
	@Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parentComment IS NULL AND c.isActive = true "
			+ "AND NOT EXISTS (SELECT ub FROM UserBlock ub WHERE (ub.blocker.id = :userId AND ub.blocked.id = c.author.id) "
			+ "     OR (ub.blocker.id = c.author.id AND ub.blocked.id = :userId)) "
			+ "ORDER BY c.createdAt ASC")
	Page<Comment> findVisibleTopLevelComments(@Param("postId") Integer postId, @Param("userId") Integer userId,
			org.springframework.data.domain.Pageable pageable);

	// Bir yoruma verilen cevaplar (aynı engel filtresiyle)
	@Query("SELECT c FROM Comment c WHERE c.parentComment.id = :parentCommentId AND c.isActive = true "
			+ "AND NOT EXISTS (SELECT ub FROM UserBlock ub WHERE (ub.blocker.id = :userId AND ub.blocked.id = c.author.id) "
			+ "     OR (ub.blocker.id = c.author.id AND ub.blocked.id = :userId)) "
			+ "ORDER BY c.createdAt ASC")
	Page<Comment> findVisibleReplies(@Param("parentCommentId") Integer parentCommentId,
			@Param("userId") Integer userId, org.springframework.data.domain.Pageable pageable);

	// Hesap silme: kullanıcının yazdığı yorumlar VE kullanıcının postlarına gelen
	// tüm yorumlar (FK kısıtı için postlar silinmeden önce temizlenir).
	@Modifying
	@Transactional
	@Query("DELETE FROM Comment c WHERE c.author.id = :userId "
			+ "OR c.post.id IN (SELECT p.id FROM Post p WHERE p.author.id = :userId)")
	void deleteAllByAuthorIdOrAuthoredPosts(@Param("userId") Integer userId);
}
