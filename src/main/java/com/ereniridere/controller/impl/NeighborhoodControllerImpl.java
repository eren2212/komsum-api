package com.ereniridere.controller.impl;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.INeighborhoodController;
import com.ereniridere.dto.response.User.DtoNeighborhood;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.service.impl.NeighborhoodServiceImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class NeighborhoodControllerImpl extends BaseController implements INeighborhoodController {

	private final NeighborhoodServiceImpl neighborhoodService;

	// 1. İlçeleri Getir: GET /api/locations/districts?city=Konya
	@GetMapping("/districts")
	@Override
	public RootEntity<List<String>> getDistricts(@RequestParam(defaultValue = "Konya") String city) {
		List<String> districts = neighborhoodService.getDistrictsByCity(city);
		return ok(districts);
	}

	// 2. Mahalleleri Getir: GET /api/locations/neighborhoods?district=Meram
	@GetMapping("/neighborhoods")
	@Override
	public RootEntity<List<DtoNeighborhood>> getNeighborhoods(@RequestParam String district) {
		List<DtoNeighborhood> neighborhoods = neighborhoodService.getNeighborhoodsByDistrict(district);
		return ok(neighborhoods);
	}
}