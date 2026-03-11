package com.ereniridere.service;

import java.util.List;

import com.ereniridere.dto.request.Merchant.DtoCreateMerchant;
import com.ereniridere.dto.response.Merchant.DtoMerchant;

public interface IMerchantService {

	public DtoMerchant createMerchantProfile(Integer userId, DtoCreateMerchant request);

	public List<DtoMerchant> getNeighborhoodMerchants(Integer userId);

	public DtoMerchant getMerchantProfile(Integer userId);

}
