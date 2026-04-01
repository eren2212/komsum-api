package com.ereniridere.service;

import java.util.List;

import com.ereniridere.dto.response.User.DtoNeighborhood;

public interface INeighborhoodService {

	List<String> getDistrictsByCity(String city);

	List<DtoNeighborhood> getNeighborhoodsByDistrict(String district);
}