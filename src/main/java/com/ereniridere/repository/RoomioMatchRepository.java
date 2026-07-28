package com.ereniridere.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.RoomioMatch;

@Repository
public interface RoomioMatchRepository extends JpaRepository<RoomioMatch, Integer> {

	@Query("SELECT m FROM RoomioMatch m WHERE "
			+ "(m.userA.id = :u1 AND m.userB.id = :u2) OR (m.userA.id = :u2 AND m.userB.id = :u1)")
	Optional<RoomioMatch> findMatchBetweenUsers(@Param("u1") Integer user1Id, @Param("u2") Integer user2Id);

	// Hesap silme: kullanıcının taraf olduğu tüm eşleşmeleri sil.
	@Modifying
	@Transactional
	@Query("DELETE FROM RoomioMatch m WHERE m.userA.id = :userId OR m.userB.id = :userId")
	void deleteAllByParticipant(@Param("userId") Integer userId);
}
