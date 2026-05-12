package com.ereniridere.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.client.TurkiyeApiClient;
import com.ereniridere.dto.external.ExternalNeighborhood;
import com.ereniridere.dto.response.User.DtoCity;
import com.ereniridere.dto.response.User.DtoDistrict;
import com.ereniridere.dto.response.User.DtoNeighborhood;
import com.ereniridere.entity.Neighborhood;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.NeighborhoodRepository;
import com.ereniridere.service.INeighborhoodService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NeighborhoodServiceImpl implements INeighborhoodService {

	private final TurkiyeApiClient turkiyeApiClient;
	private final NeighborhoodRepository neighborhoodRepository;

	// ─── İller ───────────────────────────────────────────────────────────────

	@Override
	@Cacheable("cities")
	public List<DtoCity> getCities() {
		return turkiyeApiClient.fetchCities().stream()
				.map(p -> new DtoCity(p.getId(), p.getName()))
				.sorted((a, b) -> a.getName().compareTo(b.getName()))
				.toList();
	}

	// ─── İlçeler ─────────────────────────────────────────────────────────────

	@Override
	@Cacheable(value = "districts", key = "#cityId")
	public List<DtoDistrict> getDistrictsByCityId(Integer cityId) {
		var districts = turkiyeApiClient.fetchDistrictsByCityId(cityId);
		if (districts.isEmpty()) return Collections.emptyList();
		return districts.stream()
				.map(d -> new DtoDistrict(d.getId(), d.getName()))
				.sorted((a, b) -> a.getName().compareTo(b.getName()))
				.toList();
	}

	// ─── Mahalleler ───────────────────────────────────────────────────────────

	@Override
	@Cacheable(value = "neighborhoods", key = "#districtId")
	public List<DtoNeighborhood> getNeighborhoodsByDistrictId(Integer districtId) {
		var neighborhoods = turkiyeApiClient.fetchNeighborhoodsByDistrictId(districtId);
		if (neighborhoods.isEmpty()) return Collections.emptyList();
		return neighborhoods.stream()
				.map(this::toDto)
				.sorted((a, b) -> a.getName().compareTo(b.getName()))
				.toList();
	}

	// ─── Kayıt akışı: upsert ─────────────────────────────────────────────────

	@Override
	@Transactional
	public Neighborhood getOrCreate(Integer externalId) {
		return neighborhoodRepository.findById(externalId).orElseGet(() -> {
			ExternalNeighborhood ext = turkiyeApiClient.fetchNeighborhoodById(externalId)
					.orElseThrow(() -> new BaseException(
							new ErrorMessage(MessageType.NO_RECORD_EXIST,
									"Mahalle bulunamadı (id=" + externalId + ")")));
			return neighborhoodRepository.save(buildEntity(ext));
		});
	}

	// ─── Yardımcılar ─────────────────────────────────────────────────────────

	private DtoNeighborhood toDto(ExternalNeighborhood ext) {
		DtoNeighborhood dto = new DtoNeighborhood();
		dto.setId(ext.getId());
		dto.setName(ext.getName());
		if (ext.getDistrict() != null) {
			dto.setDistrict(ext.getDistrict().getName());
			if (ext.getDistrict().getProvince() != null) {
				dto.setCity(ext.getDistrict().getProvince().getName());
			}
		}
		return dto;
	}

	private Neighborhood buildEntity(ExternalNeighborhood ext) {
		String districtName = ext.getDistrict() != null ? ext.getDistrict().getName() : null;
		String cityName = (ext.getDistrict() != null && ext.getDistrict().getProvince() != null)
				? ext.getDistrict().getProvince().getName()
				: null;
		return Neighborhood.builder()
				.id(ext.getId())
				.name(ext.getName())
				.district(districtName)
				.city(cityName)
				.build();
	}
}
