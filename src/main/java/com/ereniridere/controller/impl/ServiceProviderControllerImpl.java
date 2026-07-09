package com.ereniridere.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.IServiceProviderController;
import com.ereniridere.dto.request.serviceprovider.DtoCreateServiceProvider;
import com.ereniridere.dto.request.serviceprovider.DtoUpdateServiceProvider;
import com.ereniridere.dto.response.serviceprovider.DtoServiceProvider;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.entity.enums.ServiceCategory;
import com.ereniridere.service.IServiceProviderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/service-providers")
public class ServiceProviderControllerImpl extends BaseController implements IServiceProviderController {

	@Autowired
	private IServiceProviderService serviceProviderService;

	@PostMapping(path = "/create")
	@Override
	public RootEntity<DtoServiceProvider> createServiceProviderProfile(
			@Valid @RequestBody DtoCreateServiceProvider request) {

		Integer userId = currentUserId();

		return ok(serviceProviderService.createServiceProviderProfile(userId, request));
	}

	@GetMapping(path = "/directory")
	@Override
	public RootEntity<List<DtoServiceProvider>> getDirectory(
			@RequestParam(value = "category", required = false) ServiceCategory category) {

		Integer userId = currentUserId();

		return ok(serviceProviderService.getNeighborhoodServiceProviders(userId, category));
	}

	// Konuma bağlı (km/radius) listeleme — lat/lng gönderilmezse servis mahalle bazlı akışa düşer.
	@GetMapping(path = "/nearby")
	@Override
	public RootEntity<Page<DtoServiceProvider>> getNearby(@RequestParam(required = false) Double lat,
			@RequestParam(required = false) Double lng, @RequestParam(defaultValue = "10000") Integer radius,
			@RequestParam(required = false) ServiceCategory category,
			@RequestParam(defaultValue = "0") int pageNo, @RequestParam(defaultValue = "10") int pageSize) {

		Integer userId = currentUserId();

		return ok(serviceProviderService.getNearbyServiceProviders(userId, lat, lng, radius, category, pageNo,
				pageSize));
	}

	@GetMapping(path = "/{userId}")
	@Override
	public RootEntity<DtoServiceProvider> getServiceProviderProfile(@PathVariable(value = "userId") Integer userId) {

		return ok(serviceProviderService.getServiceProviderProfile(userId));
	}

	@PostMapping(path = "/me/update")
	@Override
	public RootEntity<DtoServiceProvider> updateServiceProviderProfile(
			@RequestBody DtoUpdateServiceProvider request) {

		Integer userId = currentUserId();

		return ok(serviceProviderService.updateServiceProviderProfile(userId, request));
	}

	@GetMapping(path = "/me")
	@Override
	public RootEntity<DtoServiceProvider> getMyServiceProviderProfile() {

		Integer userId = currentUserId();

		return ok(serviceProviderService.getMyServiceProviderProfile(userId));
	}

	@DeleteMapping(path = "/me/delete")
	@Override
	public RootEntity<Boolean> deleteMyServiceProviderProfile() {

		Integer userId = currentUserId();

		return ok(serviceProviderService.deleteMyServiceProviderProfile(userId));
	}

	private Integer currentUserId() {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return currentUser.getId();
	}

}
