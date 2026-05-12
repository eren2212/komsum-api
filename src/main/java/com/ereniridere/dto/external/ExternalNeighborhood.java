package com.ereniridere.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalNeighborhood {

	private Integer id;
	private String name;
	private ExternalDistrict district;
}
