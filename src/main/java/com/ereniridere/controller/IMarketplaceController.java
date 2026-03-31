package com.ereniridere.controller;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.market.DtoCreateListing;
import com.ereniridere.dto.response.market.DtoListing;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.enums.ListingStatus;

public interface IMarketplaceController {

	public RootEntity<DtoListing> createListing(DtoCreateListing request);

	public RootEntity<Page<DtoListing>> getListings(int pageNo, int pageSize);

	public RootEntity<Page<DtoListing>> getMyListings(int pageNo, int pageSize);

	RootEntity<DtoListing> updateStatus(Integer id, ListingStatus status);

}
