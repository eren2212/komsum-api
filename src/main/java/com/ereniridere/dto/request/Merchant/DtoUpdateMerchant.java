package com.ereniridere.dto.request.Merchant;

import lombok.Data;

@Data
public class DtoUpdateMerchant {
	// Güncelleme işleminde her alan zorunlu (@NotBlank) olmayabilir.
	// Adam sadece açıklamasını değiştirmek isteyebilir. O yüzden esnek bırakıyoruz.
	private String shopName;
	private String phone;
	private String description;
	private String profileImageUrl;

	// Ekranda "Kadıköy, İstanbul" yazıyor. Esnaf dükkanını taşırsa mahalleyi de
	// güncelleriz.
	private Integer neighborhoodId;

	// Dükkanını taşıyan esnaf harita konumunu da güncelleyebilir. Opsiyonel:
	// ikisi birden gelirse geoLocation güncellenir, gelmezse mevcut konum korunur.
	private Double latitude;
	private Double longitude;
}