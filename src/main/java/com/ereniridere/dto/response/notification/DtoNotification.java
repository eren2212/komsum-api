package com.ereniridere.dto.response.notification;

import java.time.LocalDateTime;

import com.ereniridere.entity.enums.NotificationType;
import com.ereniridere.entity.enums.RelatedEntityType;

import lombok.Data;

@Data
public class DtoNotification {

	private Long id;

	private NotificationType type;

	private String title;

	private String body;

	private RelatedEntityType relatedEntityType;

	private Long relatedEntityId;

	private Integer actorId;

	private String actorFirstName;

	private String actorLastName;

	private String actorAvatarUrl;

	private Boolean isRead;

	private LocalDateTime createdAt;
}
