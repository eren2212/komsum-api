package com.ereniridere.dto.response.User;

import lombok.Data;

@Data
public class DtoUserProfile {

	private Integer id;

	private String firstname;

	private String lastname;

	private String email;

	private DtoNeighborhood neighborhood;

	private boolean isVerifiedNeighbor;

	private Integer karmaScore = 0;

}
