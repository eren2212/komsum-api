package com.ereniridere.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comments")
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	// Yorum metni (Uzun olabileceği için TEXT yapıyoruz)
	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	// 1. Kanca: Bu yorum hangi Post'un altına yapıldı?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	// 2. Kanca: Bu yorumu kim yazdı?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User author;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	// Yorumu silersek false yapacağız (Soft Delete)
	@Builder.Default
	private boolean isActive = true;

	// 3. Kanca: Bu yorum başka bir yoruma cevapsa, o yorum hangisi?
	// Ebeveyn silinirse (hesap silme cascade'i) cevap kaybolmasın, sadece top-level'a yükselsin.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_comment_id")
	@OnDelete(action = OnDeleteAction.SET_NULL)
	private Comment parentComment;

	// Bu yoruma verilen aktif cevap sayısı (sadece top-level yorumlarda anlamlı)
	@Formula("(SELECT COUNT(*) FROM comments c WHERE c.parent_comment_id = id AND c.is_active = true)")
	private Integer replyCount;
}