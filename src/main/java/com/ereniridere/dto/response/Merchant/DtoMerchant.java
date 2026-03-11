package com.ereniridere.dto.response.Merchant;

import lombok.Data;

@Data
public class DtoMerchant {
	private Integer id;

	private String shopName;

	private String category;

	private String phone;

	private String address;

	private String description;

	private boolean isVerified;

	private String ownerFirstName;

	private String ownerLastName;
}
