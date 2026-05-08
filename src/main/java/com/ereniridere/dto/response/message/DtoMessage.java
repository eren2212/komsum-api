package com.ereniridere.dto.response.message;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DtoMessage {
	private Integer id;
	private Integer senderId;
	private String content;
	private LocalDateTime createdAt;
}