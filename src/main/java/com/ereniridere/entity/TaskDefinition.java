package com.ereniridere.entity;

import com.ereniridere.entity.enums.TaskType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bir görev tipinin tanımı: başlığı, açıklaması ve kaç puan kazandırdığı.
 * Uygulama açılışında {@code TaskDefinitionSeeder} ile idempotent seed edilir;
 * puan değerleri sonradan DB üzerinden ayarlanabilir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "task_definitions")
public class TaskDefinition {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true)
	private TaskType type;

	@Column(nullable = false)
	private String title;

	@Column(length = 300)
	private String description;

	@Column(name = "points_value", nullable = false)
	private Integer pointsValue;

	@Builder.Default
	@Column(nullable = false)
	private boolean active = true;
}
