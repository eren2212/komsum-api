package com.ereniridere.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {

	Optional<PostLike> findByPostIdAndUserId(Integer postId, Integer userId);

	// 🚨 YENİ EKLENEN SİHİRLİ METOTLAR 🚨

	// 1. Kalp kırmızı mı olacak? (Veritabanında böyle bir satır var mı diye bakar,
	// true/false döner)
	boolean existsByPostIdAndUserId(Integer postId, Integer userId);

	// 2. Toplam kaç kişi beğenmiş? (Veritabanındaki satırları sayıp Integer döner)
	Integer countByPostId(Integer postId);

	// Hesap silme: kullanıcının verdiği beğeniler VE kullanıcının postlarına gelen
	// tüm beğeniler (FK kısıtı için postlar silinmeden önce temizlenir).
	@Modifying
	@Transactional
	@Query("DELETE FROM PostLike pl WHERE pl.user.id = :userId "
			+ "OR pl.post.id IN (SELECT p.id FROM Post p WHERE p.author.id = :userId)")
	void deleteAllByUserIdOrAuthoredPosts(@Param("userId") Integer userId);
}
