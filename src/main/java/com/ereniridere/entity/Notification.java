package com.ereniridere.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ereniridere.entity.enums.NotificationType;
import com.ereniridere.entity.enums.RelatedEntityType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications", indexes = {
		@Index(name = "idx_notif_recipient_created", columnList = "recipient_id, created_at DESC"),
		@Index(name = "idx_notif_recipient_unread", columnList = "recipient_id, is_read")
})
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Bildirimi ALAN kullanıcı
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient_id", nullable = false)
	private User recipient;

	// Bildirimi TETİKLEYEN kullanıcı (post atan, mesaj gönderen vs.)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id")
	private User actor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationType type;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 500)
	private String body;

	@Enumerated(EnumType.STRING)
	@Column(name = "related_entity_type")
	private RelatedEntityType relatedEntityType;

	@Column(name = "related_entity_id")
	private Long relatedEntityId;

	@Builder.Default
	@Column(name = "is_read", nullable = false)
	private Boolean isRead = false;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
}
