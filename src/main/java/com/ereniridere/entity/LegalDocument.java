package com.ereniridere.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ereniridere.entity.enums.LegalDocumentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KVKK açık rıza ve aydınlatma metni gibi yasal metinleri tutar. Mobil kayıt
 * ekranında kullanıcıya gösterilir. Metinler güncellendikçe yeni versiyonlar
 * eklenip eskisi {@code active = false} yapılabilir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "legal_documents")
public class LegalDocument {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LegalDocumentType type;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	// Metnin sürümü — onay kaydında hangi sürümün kabul edildiğini izlemek için.
	@Column(nullable = false)
	private Integer version;

	// Şu an yürürlükte olan metin mi?
	@Builder.Default
	@Column(nullable = false)
	private boolean active = true;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
}
