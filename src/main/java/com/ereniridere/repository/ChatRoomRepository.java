package com.ereniridere.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {

	// İki kullanıcı arasında daha önce açılmış bir oda var mı?
	// (A kullanıcısı mı başlattı yoksa B mi fark etmeksizin bulur)
	@Query("SELECT c FROM ChatRoom c WHERE " + "(c.user1.id = :u1 AND c.user2.id = :u2) OR "
			+ "(c.user1.id = :u2 AND c.user2.id = :u1)")
	Optional<ChatRoom> findChatRoomBetweenUsers(@Param("u1") Integer user1Id, @Param("u2") Integer user2Id);

	// Benim (userId) içinde bulunduğum tüm odaları, son mesajlaşma tarihine göre
	// (en yeni en üstte) getirir. Taraflar arasında (herhangi bir yönde) engel varsa
	// oda gelen kutusunda görünmez.
	@Query("SELECT c FROM ChatRoom c WHERE (c.user1.id = :userId OR c.user2.id = :userId) "
			+ "AND NOT EXISTS (SELECT ub FROM UserBlock ub WHERE (ub.blocker.id = c.user1.id AND ub.blocked.id = c.user2.id) "
			+ "     OR (ub.blocker.id = c.user2.id AND ub.blocked.id = c.user1.id)) "
			+ "ORDER BY c.lastMessageAt DESC")
	Page<ChatRoom> getUserChatRooms(@Param("userId") Integer userId, Pageable pageable);

	// Hesap silme: kullanıcının taraf olduğu tüm sohbet odalarını sil.
	// (Önce bu odalardaki mesajlar temizlenmiş olmalı — FK kısıtı.)
	@Modifying
	@Transactional
	@Query("DELETE FROM ChatRoom c WHERE c.user1.id = :userId OR c.user2.id = :userId")
	void deleteAllByParticipant(@Param("userId") Integer userId);
}