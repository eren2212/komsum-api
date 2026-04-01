package com.ereniridere.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;

import com.ereniridere.dto.response.User.DtoNeighborhood;
import com.ereniridere.entity.RootEntity;

public interface INeighborhoodController {

	RootEntity<List<String>> getDistricts(@RequestParam String city);

	RootEntity<List<DtoNeighborhood>> getNeighborhoods(@RequestParam String district);
}