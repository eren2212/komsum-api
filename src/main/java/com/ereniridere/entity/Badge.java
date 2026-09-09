package com.ereniridere.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Toplam puan eşiğine ulaşınca kazanılan rozet. Sabit liste olarak
 * {@code BadgeSeeder} ile seed edilir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "badges")
public class Badge {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(length = 300)
	private String description;

	@Column(name = "point_threshold", nullable = false)
	private Integer pointThreshold;

	@Column(name = "icon_url")
	private String iconUrl;
}
