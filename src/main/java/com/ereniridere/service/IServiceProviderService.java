package com.ereniridere.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.serviceprovider.DtoCreateServiceProvider;
import com.ereniridere.dto.request.serviceprovider.DtoUpdateServiceProvider;
import com.ereniridere.dto.response.serviceprovider.DtoServiceProvider;
import com.ereniridere.entity.enums.ServiceCategory;

public interface IServiceProviderService {

	public DtoServiceProvider createServiceProviderProfile(Integer userId, DtoCreateServiceProvider request);

	// category null ise mahalledeki tüm onaylı ustalar, doluysa kategoriye göre filtreli
	public List<DtoServiceProvider> getNeighborhoodServiceProviders(Integer userId, ServiceCategory category);

	// Konuma bağlı (km/radius) listeleme — verilen noktanın 'radius' metre çevresindeki
	// onaylı ustalar, en yakın en üstte. lat/lng yoksa mahalle bazlı akışa düşer.
	public Page<DtoServiceProvider> getNearbyServiceProviders(Integer userId, Double lat, Double lng, Integer radius,
			ServiceCategory category, int pageNo, int pageSize);

	public DtoServiceProvider getServiceProviderProfile(Integer userId);

	public DtoServiceProvider getMyServiceProviderProfile(Integer userId);

	public DtoServiceProvider updateServiceProviderProfile(Integer userId, DtoUpdateServiceProvider request);

	public boolean deleteMyServiceProviderProfile(Integer userId);

}
