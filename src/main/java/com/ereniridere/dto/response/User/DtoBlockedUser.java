package com.ereniridere.dto.response.User;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DtoBlockedUser {

	private Integer id;

	private String firstname;

	private String lastname;

	private String avatarUrl;

	// Engellendiği an
	private LocalDateTime blockedAt;
}
