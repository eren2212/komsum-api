package com.ereniridere.dto.response.message;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DtoChatRoom {
	private Integer id;
	private Integer otherUserId;
	private String otherUserFirstName;
	private String otherUserLastName;
	private String otherUserAvatarUrl;
	private LocalDateTime lastMessageAt;
	private String lastMessageContent;
	private int unreadCount;
}