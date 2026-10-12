package com.ereniridere.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.UserBlock;

public interface UserBlockRepository extends JpaRepository<UserBlock, Integer> {

	boolean existsByBlockerIdAndBlockedId(Integer blockerId, Integer blockedId);

	// İki yönlü kontrol: a, b'yi engellemiş VEYA b, a'yı engellemiş.
	// Sohbet başlatma / mesaj gönderme / bildirim gibi tekil kontrollerde kullanılır.
	@Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM UserBlock b "
			+ "WHERE (b.blocker.id = :a AND b.blocked.id = :b) OR (b.blocker.id = :b AND b.blocked.id = :a)")
	boolean existsBlockBetween(@Param("a") Integer a, @Param("b") Integer b);

	// Engellediklerim listesi (en yeni engel en üstte)
	@Query(value = "SELECT b FROM UserBlock b JOIN FETCH b.blocked WHERE b.blocker.id = :blockerId "
			+ "ORDER BY b.createdAt DESC", countQuery = "SELECT COUNT(b) FROM UserBlock b WHERE b.blocker.id = :blockerId")
	Page<UserBlock> findByBlockerIdWithBlocked(@Param("blockerId") Integer blockerId, Pageable pageable);

	@Modifying
	@Transactional
	@Query("DELETE FROM UserBlock b WHERE b.blocker.id = :blockerId AND b.blocked.id = :blockedId")
	void deleteByBlockerIdAndBlockedId(@Param("blockerId") Integer blockerId, @Param("blockedId") Integer blockedId);

	// Hesap silme: kullanıcının engellediği VE kullanıcıyı engelleyen tüm kayıtlar.
	@Modifying
	@Transactional
	@Query("DELETE FROM UserBlock b WHERE b.blocker.id = :userId OR b.blocked.id = :userId")
	void deleteAllByUser(@Param("userId") Integer userId);
}
