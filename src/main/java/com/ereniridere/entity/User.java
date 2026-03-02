package com.ereniridere.entity;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: Getter, Setter, toString hepsini otomatik yazar.
@Builder // Lombok: Nesne üretirken 'builder' pattern kullanmamızı sağlar.
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users") // 🚨 SENIOR DETAYI: PostgreSQL'de "user" kelimesi özel (reserved) bir
						// kelimedir. Tablo adını o yüzden "users" yapıyoruz, yoksa Supabase hata
						// fırlatır!
public class User implements UserDetails {

	@Id // Birincil anahtar (Primary Key)
	@GeneratedValue(strategy = GenerationType.IDENTITY) // ID'yi Supabase otomatik artırsın (1, 2, 3...)
	private Integer id;

	private String firstname;
	private String lastname;

	@Column(unique = true) // Aynı e-posta ile iki kişi kayıt olamasın
	private String email;

	private String password;

	@Enumerated(EnumType.STRING) // Veritabanına sayı (0,1) olarak değil, metin ("USER", "ADMIN") olarak kaydet.
	private Role role;

	// --- BURADAN AŞAĞISI SPRING SECURITY'NİN ZORUNLU KILDIĞI METODLAR ---

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// Kullanıcının rolünü Spring Security'nin anladığı formata çevirip veriyoruz.
		return List.of(new SimpleGrantedAuthority(role.name()));
	}

	@Override
	public String getPassword() {
		return password; // Şifreyi dön
	}

	@Override
	public String getUsername() {
		// 🚨 ÖNEMLİ: Spring Security kullanıcı adı (username) bekler ama bizim
		// sistemimizde girişler E-posta ile yapılıyor. O yüzden burada email dönüyoruz!
		return email;
	}

	// Hesapların süresi doldu mu, kilitli mi gibi kontroller. Şimdilik hepsine
	// 'true' (aktif) diyoruz.
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