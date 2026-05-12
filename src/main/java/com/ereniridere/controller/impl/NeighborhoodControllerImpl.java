package com.ereniridere.controller.impl;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.INeighborhoodController;
import com.ereniridere.dto.response.User.DtoCity;
import com.ereniridere.dto.response.User.DtoDistrict;
import com.ereniridere.dto.response.User.DtoNeighborhood;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.service.INeighborhoodService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class NeighborhoodControllerImpl extends BaseController implements INeighborhoodController {

	private final INeighborhoodService neighborhoodService;

	/** GET /api/locations/cities */
	@GetMapping("/cities")
	@Override
	public RootEntity<List<DtoCity>> getCities() {
		return ok(neighborhoodService.getCities());
	}

	/** GET /api/locations/districts?cityId=34 */
	@GetMapping("/districts")
	@Override
	public RootEntity<List<DtoDistrict>> getDistricts(@RequestParam Integer cityId) {
		return ok(neighborhoodService.getDistrictsByCityId(cityId));
	}

	/** GET /api/locations/neighborhoods?districtId=100 */
	@GetMapping("/neighborhoods")
	@Override
	public RootEntity<List<DtoNeighborhood>> getNeighborhoods(@RequestParam Integer districtId) {
		return ok(neighborhoodService.getNeighborhoodsByDistrictId(districtId));
	}
}
