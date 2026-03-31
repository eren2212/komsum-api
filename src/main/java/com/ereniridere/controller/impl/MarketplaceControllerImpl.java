package com.ereniridere.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.IMarketplaceController;
import com.ereniridere.dto.request.market.DtoCreateListing;
import com.ereniridere.dto.response.market.DtoListing;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.entity.enums.ListingStatus;
import com.ereniridere.service.impl.MarketplaceServiceImpl;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/marketplace")

public class MarketplaceControllerImpl extends BaseController implements IMarketplaceController {

	@Autowired
	private MarketplaceServiceImpl marketplaceService;

	// 1. İlan Ekle (POST /api/marketplace)
	@PostMapping
	public RootEntity<DtoListing> createListing(@Valid @RequestBody DtoCreateListing request) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		DtoListing response = marketplaceService.createListing(currentUser.getId(), request);
		return ok(response);
	}

	// 2. Mahalledeki İlanları Getir (GET
	// /api/marketplace/feed?pageNo=0&pageSize=10)
	@GetMapping("/feed")
	public RootEntity<Page<DtoListing>> getListings(@RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "10") int pageSize) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Page<DtoListing> response = marketplaceService.getNeighborhoodListings(currentUser.getId(), pageNo, pageSize);
		return ok(response);
	}

	// 3. Sadece Kendi Paylaştığım İlanları Getir (GET
	// /api/marketplace/me?pageNo=0&pageSize=10)
	@GetMapping("/me")
	@Override
	public RootEntity<Page<DtoListing>> getMyListings(@RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "10") int pageSize) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Page<DtoListing> response = marketplaceService.getMyListings(currentUser.getId(), pageNo, pageSize);
		return ok(response);
	}

	// 4. İlan Durumunu Güncelle (PATCH /api/marketplace/{id}/status?status=SOLD)
	@PatchMapping("/{id}/status")
	@Override
	public RootEntity<DtoListing> updateStatus(@PathVariable Integer id, @RequestParam ListingStatus status) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		DtoListing response = marketplaceService.updateListingStatus(currentUser.getId(), id, status);

		return ok(response);
	}
}