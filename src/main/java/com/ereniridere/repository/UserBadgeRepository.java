package com.ereniridere.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ereniridere.entity.UserBadge;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Integer> {

	@Query("SELECT ub FROM UserBadge ub JOIN FETCH ub.badge WHERE ub.user.id = :userId ORDER BY ub.earnedAt ASC")
	List<UserBadge> findByUserIdWithBadge(@Param("userId") Integer userId);

	@Query("SELECT ub.badge.id FROM UserBadge ub WHERE ub.user.id = :userId")
	List<Integer> findBadgeIdsByUserId(@Param("userId") Integer userId);

	long countByUserId(Integer userId);
}
