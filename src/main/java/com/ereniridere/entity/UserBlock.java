package com.ereniridere.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bir kullanıcının başka bir kullanıcıyı engellemesi. Engelleme iki yönlü
 * uygulanır: blocker ve blocked birbirinin içeriğini görmez ve mesajlaşamaz
 * (filtreler her iki yönü de kontrol eder). Kayıt ise kimin engellediğini
 * tutar — engeli sadece engelleyen kaldırabilir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_blocks", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "blocker_id", "blocked_id" }) }, indexes = {
				@Index(name = "idx_user_blocks_blocked", columnList = "blocked_id") })
public class UserBlock {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	// Engelleyen kullanıcı
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "blocker_id", nullable = false)
	private User blocker;

	// Engellenen kullanıcı
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "blocked_id", nullable = false)
	private User blocked;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
}
