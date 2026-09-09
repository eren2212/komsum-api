package com.ereniridere.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ereniridere.entity.enums.TaskType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bir kullanıcının belirli bir günde belirli bir görevi tamamladığını gösterir.
 * Unique constraint "günde bir kez" kuralını DB seviyesinde garanti eder —
 * etkinliğe gir/çık gibi tekrarlanabilir aksiyonlarda puan çiftliğini engeller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "task_completions", uniqueConstraints = @UniqueConstraint(name = "uk_task_completion_user_type_date", columnNames = {
		"user_id", "task_type", "completion_date" }))
public class TaskCompletion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_type", nullable = false)
	private TaskType taskType;

	@Column(name = "completion_date", nullable = false)
	private LocalDate completionDate;

	@Column(name = "points_awarded", nullable = false)
	private Integer pointsAwarded;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
}
