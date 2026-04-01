package com.ereniridere.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.response.User.DtoNeighborhood;
import com.ereniridere.entity.Neighborhood;
import com.ereniridere.repository.NeighborhoodRepository;
import com.ereniridere.service.INeighborhoodService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NeighborhoodServiceImpl implements INeighborhoodService {

	private final NeighborhoodRepository neighborhoodRepository;

	@Override
	public List<String> getDistrictsByCity(String city) {
		// Doğrudan Repository'den eşsiz ilçe isimleri String listesi olarak dönecek
		return neighborhoodRepository.findDistinctDistrictsByCity(city);
	}

	@Override
	public List<DtoNeighborhood> getNeighborhoodsByDistrict(String district) {
		// İlçeye ait mahalleleri çekip, mobilde göstermek için DTO listesine
		// çeviriyoruz
		List<Neighborhood> neighborhoods = neighborhoodRepository.findByDistrictOrderByNameAsc(district);

		return neighborhoods.stream().map(neighborhood -> {
			DtoNeighborhood dto = new DtoNeighborhood();
			BeanUtils.copyProperties(neighborhood, dto);
			return dto;
		}).collect(Collectors.toList());
	}
}