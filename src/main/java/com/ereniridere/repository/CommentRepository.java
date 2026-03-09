package com.ereniridere.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ereniridere.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
	Page<Comment> findByPostIdAndIsActiveTrueOrderByCreatedAtAsc(Integer postId,
			org.springframework.data.domain.Pageable pageable);
}
