package com.ereniridere.dto.external;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalProvince {

	private Integer id;
	private String name;
	private List<ExternalDistrict> districts;
}
