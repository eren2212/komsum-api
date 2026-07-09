package com.ereniridere.controller;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.serviceprovider.DtoCreateServiceProvider;
import com.ereniridere.dto.request.serviceprovider.DtoUpdateServiceProvider;
import com.ereniridere.dto.response.serviceprovider.DtoServiceProvider;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.enums.ServiceCategory;

public interface IServiceProviderController {

	public RootEntity<DtoServiceProvider> createServiceProviderProfile(DtoCreateServiceProvider request);

	public RootEntity<List<DtoServiceProvider>> getDirectory(ServiceCategory category);

	public RootEntity<Page<DtoServiceProvider>> getNearby(Double lat, Double lng, Integer radius,
			ServiceCategory category, int pageNo, int pageSize);

	public RootEntity<DtoServiceProvider> getServiceProviderProfile(Integer userId);

	public RootEntity<DtoServiceProvider> updateServiceProviderProfile(DtoUpdateServiceProvider request);

	public RootEntity<DtoServiceProvider> getMyServiceProviderProfile();

	public RootEntity<Boolean> deleteMyServiceProviderProfile();

}
