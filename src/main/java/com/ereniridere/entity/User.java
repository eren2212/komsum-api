package com.ereniridere.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String firstname;
	private String lastname;

	@Column(unique = true)
	private String email;

	private String password;

	private String resetOtp;

	private LocalDateTime resetOtpExpiration;

	@Enumerated(EnumType.STRING)
	private Role role;

	// --- KOMŞUM UYGULAMASI YENİ ÖZELLİKLERİ ---

	// 1. Mahalle Kancası (Veritabanında 'neighborhood_id' adında bir sütun açacak)
	@ManyToOne(fetch = FetchType.LAZY) // LAZY: Sadece mahalle bilgisini istediğimizde getir, RAM'i yorma!
	@JoinColumn(name = "neighborhood_id")
	private Neighborhood neighborhood;

	// 2. Doğrulanmış Komşu Rozeti (İlk kayıtta varsayılan olarak false/kapatık
	// gelir)
	@Builder.Default
	private boolean isVerifiedNeighbor = false;

	// 3. Yardımseverlik Puanı (İyilik yaptıkça artacak, ilk kayıtta 0)
	@Builder.Default
	private Integer karmaScore = 0;

	// 4. Spam Engellemek İçin Son Mahalle Değiştirme Tarihi
	private LocalDateTime lastNeighborhoodChange;

	// --- SPRING SECURITY METODLARI (Aşağısı eskisi gibi kalacak) ---
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(role.name()));
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}