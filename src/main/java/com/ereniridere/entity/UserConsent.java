package com.ereniridere.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ereniridere.entity.enums.LegalDocumentType;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bir kullanıcının hangi yasal metni (KVKK / Aydınlatma) hangi sürümde ve ne
 * zaman kabul ettiğini saklar. KVKK uyumu için kabul kanıtı niteliğindedir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_consents")
public class UserConsent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", nullable = false)
	private LegalDocumentType documentType;

	@Column(name = "document_version", nullable = false)
	private Integer documentVersion;

	@CreationTimestamp
	@Column(name = "accepted_at", updatable = false)
	private LocalDateTime acceptedAt;
}
