package com.ereniridere.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ereniridere.entity.TaskCompletion;
import com.ereniridere.entity.enums.TaskType;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, Integer> {

	boolean existsByUserIdAndTaskTypeAndCompletionDate(Integer userId, TaskType taskType, LocalDate completionDate);

	@Query("SELECT tc.taskType FROM TaskCompletion tc WHERE tc.user.id = :userId AND tc.completionDate = :date")
	List<TaskType> findCompletedTypes(@Param("userId") Integer userId, @Param("date") LocalDate date);
}
