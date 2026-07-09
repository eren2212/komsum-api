package com.ereniridere.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.MerchantProfile;

@Repository
public interface MerchantProfileRepository extends JpaRepository<MerchantProfile, Integer> {

	// 1. Kullanıcının zaten bir dükkanı var mı diye kontrol etmek için
	Optional<MerchantProfile> findByUserId(Integer userId);

	// 2. Mobilde "Esnaf Rehberi" sayfasını çizerken:
	// Sadece o mahalledeki ve ONAYLANMIŞ (isVerified=true) esnafları getirecek
	// efsane sorgu!
	List<MerchantProfile> findByNeighborhoodIdAndIsVerifiedTrue(Integer neighborhoodId);

	// Hesap silme: kullanıcının esnaf profilini sil.
	@Modifying
	@Transactional
	@Query("DELETE FROM MerchantProfile m WHERE m.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);

}
