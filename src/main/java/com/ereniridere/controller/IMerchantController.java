package com.ereniridere.controller;

import java.util.List;

import com.ereniridere.dto.request.Merchant.DtoCreateMerchant;
import com.ereniridere.dto.response.Merchant.DtoMerchant;
import com.ereniridere.entity.RootEntity;

public interface IMerchantController {

	public RootEntity<DtoMerchant> createMerchantProfile(DtoCreateMerchant request);

	public RootEntity<List<DtoMerchant>> getDirectory();

	public RootEntity<DtoMerchant> getMerchantProfile(Integer userId);

}
