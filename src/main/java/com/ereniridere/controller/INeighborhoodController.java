package com.ereniridere.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;

import com.ereniridere.dto.response.User.DtoCity;
import com.ereniridere.dto.response.User.DtoDistrict;
import com.ereniridere.dto.response.User.DtoNeighborhood;
import com.ereniridere.entity.RootEntity;

public interface INeighborhoodController {

	RootEntity<List<DtoCity>> getCities();

	RootEntity<List<DtoDistrict>> getDistricts(@RequestParam Integer cityId);

	RootEntity<List<DtoNeighborhood>> getNeighborhoods(@RequestParam Integer districtId);
}
