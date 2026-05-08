package com.ereniridere.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Integer> {

	Page<Message> findByChatRoomIdOrderByCreatedAtAsc(Integer chatRoomId, Pageable pageable);

	@Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :roomId AND m.sender.id != :userId AND m.isRead = false")
	long countUnreadMessages(@Param("roomId") Integer roomId, @Param("userId") Integer userId);

	@Modifying
	@Transactional
	@Query("UPDATE Message m SET m.isRead = true WHERE m.chatRoom.id = :roomId AND m.sender.id != :userId AND m.isRead = false")
	void markMessagesAsRead(@Param("roomId") Integer roomId, @Param("userId") Integer userId);
}