package com.ereniridere.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

	@Query("SELECT n FROM Notification n " +
			"LEFT JOIN FETCH n.actor a " +
			"WHERE n.recipient.id = :userId " +
			"ORDER BY n.createdAt DESC")
	Page<Notification> findByRecipientId(@Param("userId") Integer userId, Pageable pageable);

	@Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient.id = :userId AND n.isRead = false")
	long countUnreadByRecipientId(@Param("userId") Integer userId);

	@Modifying
	@Transactional
	@Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.recipient.id = :userId")
	int markAsRead(@Param("id") Long id, @Param("userId") Integer userId);

	@Modifying
	@Transactional
	@Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :userId AND n.isRead = false")
	int markAllAsRead(@Param("userId") Integer userId);

	// Hesap silme: kullanıcının alıcı VEYA aktör olduğu tüm bildirimleri sil.
	@Modifying
	@Transactional
	@Query("DELETE FROM Notification n WHERE n.recipient.id = :userId OR n.actor.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);
}
