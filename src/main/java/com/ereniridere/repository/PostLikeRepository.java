package com.ereniridere.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ereniridere.entity.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {

	Optional<PostLike> findByPostIdAndUserId(Integer postId, Integer userId);
}
