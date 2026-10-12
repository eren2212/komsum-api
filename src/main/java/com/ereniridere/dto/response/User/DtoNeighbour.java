package com.ereniridere.dto.response.User;

import lombok.Data;

@Data
public class DtoNeighbour {

	private Integer id;

	private String firstname;

	private String lastname;

	private String email;

	private String avatarUrl;

	private String bio;

	private DtoNeighborhood neighborhood;

	private boolean isVerifiedNeighbor;

	private Integer karmaScore = 0;

	// Görüntüleyen kullanıcı bu kişiyi engellemiş mi? (Profilde Engelle / Engeli Kaldır)
	private boolean blockedByMe;

}
