package com.ereniridere.dto.response.post;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DtoComment {

	private Integer id;

	private String content;

	private Integer authorId;

	private String authorFirstName;

	private String authorLastName;

	private LocalDateTime createdAt;
}
