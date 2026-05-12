package com.ereniridere.service;

import java.util.List;

import com.ereniridere.dto.response.User.DtoCity;
import com.ereniridere.dto.response.User.DtoDistrict;
import com.ereniridere.dto.response.User.DtoNeighborhood;
import com.ereniridere.entity.Neighborhood;

public interface INeighborhoodService {

	/** Tüm Türkiye illerini döner. */
	List<DtoCity> getCities();

	/** Verilen il ID'sine göre ilçeleri döner. */
	List<DtoDistrict> getDistrictsByCityId(Integer cityId);

	/** Verilen ilçe ID'sine göre mahalleleri döner. */
	List<DtoNeighborhood> getNeighborhoodsByDistrictId(Integer districtId);

	/** Kayıt akışında çağrılır: mahalle DB'de yoksa API'den çekip kaydeder. */
	Neighborhood getOrCreate(Integer externalId);
}
