package com.ereniridere.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "neighborhoods")
public class Neighborhood {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String city; // Örn: İstanbul
	private String district; // Örn: Beşiktaş
	private String name; // Örn: Sinanpaşa Mahallesi
	private String zipCode; // Örn: 34353
}