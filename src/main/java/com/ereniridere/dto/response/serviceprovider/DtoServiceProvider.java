package com.ereniridere.dto.response.serviceprovider;

import com.ereniridere.entity.enums.ServiceCategory;

import lombok.Data;

@Data
public class DtoServiceProvider {

	private Integer id;

	// Profil sahibinin kullanıcı id'si — mobil tarafta detay endpoint'i /{userId}
	// üzerinden çalıştığı için (rehber kartından doğru kullanıcıya gitmek) gerekli.
	private Integer userId;

	private String title;

	private ServiceCategory category;

	private String phone;

	private String description;

	private Integer experienceYears;

	private String priceInfo;

	private boolean available;

	private String address;

	private boolean isVerified;

	private String profileImageUrl;

	private String ownerFirstName;

	private String ownerLastName;

	// Harita konumu (mobilde pin göstermek için). Konum yoksa null.
	private Double latitude;

	private Double longitude;
}
