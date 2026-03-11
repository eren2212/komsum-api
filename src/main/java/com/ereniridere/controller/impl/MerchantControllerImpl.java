package com.ereniridere.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.IMerchantController;
import com.ereniridere.dto.request.Merchant.DtoCreateMerchant;
import com.ereniridere.dto.response.Merchant.DtoMerchant;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.service.IMerchantService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/merchants")
public class MerchantControllerImpl extends BaseController implements IMerchantController {

	@Autowired
	private IMerchantService merchantService;

	@PostMapping(path = "/create")
	@Override
	public RootEntity<DtoMerchant> createMerchantProfile(@Valid @RequestBody DtoCreateMerchant request) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Integer userId = currentUser.getId();

		return ok(merchantService.createMerchantProfile(userId, request));
	}

	@GetMapping("/directory")
	@Override
	public RootEntity<List<DtoMerchant>> getDirectory() {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Integer userId = currentUser.getId();

		return ok(merchantService.getNeighborhoodMerchants(userId));
	}

	@GetMapping("/{userId}")
	@Override
	public RootEntity<DtoMerchant> getMerchantProfile(@PathVariable(value = "userId") Integer userId) {

		return ok(merchantService.getMerchantProfile(userId));

	}

}
