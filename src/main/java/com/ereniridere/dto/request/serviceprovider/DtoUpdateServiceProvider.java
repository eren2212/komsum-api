package com.ereniridere.dto.request.serviceprovider;

import com.ereniridere.entity.enums.ServiceCategory;

import lombok.Data;

/**
 * Güncellemede her alan opsiyonel — usta sadece tek bir alanı (örn. müsaitlik)
 * değiştirmek isteyebilir. Null gelen alanlar mevcut değeri korur.
 */
@Data
public class DtoUpdateServiceProvider {

	private String title;
	private ServiceCategory category;
	private String phone;
	private String description;
	private Integer experienceYears;
	private String priceInfo;
	private Boolean available;
	private String profileImageUrl;

	// Usta bölge değiştirirse mahalle de güncellenir.
	private Integer neighborhoodId;

	// Konum güncellemesi — ikisi birlikte gelmeli; biri eksikse mevcut konum korunur.
	private Double latitude;
	private Double longitude;
}
