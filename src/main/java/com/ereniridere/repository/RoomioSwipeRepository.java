package com.ereniridere.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.RoomioSwipe;
import com.ereniridere.entity.enums.RoomioSwipeAction;

@Repository
public interface RoomioSwipeRepository extends JpaRepository<RoomioSwipe, Integer> {

	Optional<RoomioSwipe> findBySwiperIdAndSwipedId(Integer swiperId, Integer swipedId);

	// Karşı taraf beni daha önce LIKE'lamış mı? (mutual match tespiti)
	boolean existsBySwiperIdAndSwipedIdAndAction(Integer swiperId, Integer swipedId, RoomioSwipeAction action);

	// Hesap silme: kullanıcının attığı/aldığı tüm swipe kayıtlarını sil.
	@Modifying
	@Transactional
	@Query("DELETE FROM RoomioSwipe s WHERE s.swiper.id = :userId OR s.swiped.id = :userId")
	void deleteAllBySwiperIdOrSwipedId(@Param("userId") Integer userId);
}
