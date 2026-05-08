package com.ereniridere.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ereniridere.entity.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {

	// İki kullanıcı arasında daha önce açılmış bir oda var mı?
	// (A kullanıcısı mı başlattı yoksa B mi fark etmeksizin bulur)
	@Query("SELECT c FROM ChatRoom c WHERE " + "(c.user1.id = :u1 AND c.user2.id = :u2) OR "
			+ "(c.user1.id = :u2 AND c.user2.id = :u1)")
	Optional<ChatRoom> findChatRoomBetweenUsers(@Param("u1") Integer user1Id, @Param("u2") Integer user2Id);

	// Benim (userId) içinde bulunduğum tüm odaları, son mesajlaşma tarihine göre
	// (en yeni en üstte) getirir.
	@Query("SELECT c FROM ChatRoom c WHERE c.user1.id = :userId OR c.user2.id = :userId ORDER BY c.lastMessageAt DESC")
	Page<ChatRoom> getUserChatRooms(@Param("userId") Integer userId, Pageable pageable);
}