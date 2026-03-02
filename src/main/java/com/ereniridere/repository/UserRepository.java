package com.ereniridere.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ereniridere.entity.User;

@Repository
// JpaRepository<Hangi Tablo, O Tablonun ID Tipi>
public interface UserRepository extends JpaRepository<User, Integer> {

	// Spring Data JPA'nın sihri! Tek satır SQL yazmadan isimlendirme kuralıyla
	// e-postadan kullanıcı bulan metod.
	Optional<User> findByEmail(String email);

}