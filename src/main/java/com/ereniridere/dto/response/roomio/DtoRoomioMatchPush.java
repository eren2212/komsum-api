package com.ereniridere.dto.response.roomio;

import lombok.Data;

// Roomio eşleşmesinin SSE ("roomio_match" event) üzerinden anlık teslimi için.
@Data
public class DtoRoomioMatchPush {
	private Integer matchId;
	private Integer chatRoomId;
	private Integer otherUserId;
	private String otherUserFirstName;
	private String otherUserLastName;
	private String otherUserAvatarUrl;
}
