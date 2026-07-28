package com.ereniridere.dto.response.roomio;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DtoRoomioMatch {
	private Integer matchId;
	private Integer chatRoomId;
	private Integer otherUserId;
	private String otherUserFirstName;
	private String otherUserLastName;
	private String otherUserAvatarUrl;
	private String lastMessageContent;
	private LocalDateTime lastMessageAt;
	private int unreadCount;
	private LocalDateTime matchedAt;
}
