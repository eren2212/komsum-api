package com.ereniridere.dto.response.market;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ereniridere.entity.enums.ListingStatus;
import com.ereniridere.entity.enums.ListingType;

import lombok.Data;

@Data
public class DtoListing {

	private Integer id;
	private String title;
	private String imageUrl;
	private ListingType type;
	private BigDecimal price;
	private String category;
	private ListingStatus status;
	private LocalDateTime createdAt;

	// İlanı kimin açtığını bilmemiz lazım ki adam karta tıklayınca satıcıyla
	// iletişime geçebilsin
	private Integer sellerId;
	private String sellerFirstName;
	private String sellerLastName;

}
