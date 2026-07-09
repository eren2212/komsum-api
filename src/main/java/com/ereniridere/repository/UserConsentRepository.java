package com.ereniridere.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.UserConsent;

public interface UserConsentRepository extends JpaRepository<UserConsent, Integer> {

	// Hesap silme: kullanıcının tüm onay kayıtlarını sil.
	@Modifying
	@Transactional
	@Query("DELETE FROM UserConsent c WHERE c.user.id = :userId")
	void deleteAllByUserId(@Param("userId") Integer userId);
}
