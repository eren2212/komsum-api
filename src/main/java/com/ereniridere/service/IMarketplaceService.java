package com.ereniridere.service;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.market.DtoCreateListing;
import com.ereniridere.dto.response.market.DtoListing;
import com.ereniridere.entity.enums.ListingStatus;

public interface IMarketplaceService {

	public DtoListing createListing(Integer userId, DtoCreateListing request);

	public Page<DtoListing> getNeighborhoodListings(Integer userId, int pageNo, int pageSize);

	public Page<DtoListing> getMyListings(Integer userId, int pageNo, int pageSize);

	public DtoListing updateListingStatus(Integer userId, Integer listingId, ListingStatus newStatus);
}
