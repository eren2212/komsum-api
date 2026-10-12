package com.ereniridere.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ereniridere.entity.enums.ReportReason;
import com.ereniridere.entity.enums.ReportStatus;
import com.ereniridere.entity.enums.ReportedEntityType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reports", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "user_id", "reported_entity_type", "reported_entity_id" }) })
public class Report {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	// Şikayeti yapan kullanıcı
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User reporter;

	// Polymorphic hedef: Notification.relatedEntityType/relatedEntityId ile aynı desen
	@Enumerated(EnumType.STRING)
	@Column(name = "reported_entity_type", nullable = false)
	private ReportedEntityType reportedEntityType;

	@Column(name = "reported_entity_id", nullable = false)
	private Integer reportedEntityId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReportReason reason;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReportStatus status = ReportStatus.PENDING;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
}
