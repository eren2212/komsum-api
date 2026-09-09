package com.ereniridere.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ereniridere.entity.TaskDefinition;
import com.ereniridere.entity.enums.TaskType;

public interface TaskDefinitionRepository extends JpaRepository<TaskDefinition, Integer> {

	List<TaskDefinition> findByActiveTrue();

	Optional<TaskDefinition> findByTypeAndActiveTrue(TaskType type);

	boolean existsByType(TaskType type);
}
