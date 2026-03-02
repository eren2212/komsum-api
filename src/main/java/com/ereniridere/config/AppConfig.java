package com.ereniridere.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ereniridere.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Configuration // Spring'e "Ayağa kalkarken buraya uğra, sana vereceğim aletleri hafızana al"
				// diyoruz.
@RequiredArgsConstructor
public class AppConfig {

	// 🚨 DİKKAT: Burada UserRepository henüz oluşturmadığımız için IDE kızarabilir.
	// Bu çok normal, senior bir geliştirici olarak bilerek bu adımı atıyoruz,
	// birazdan toparlayacağız!
	private final UserRepository userRepository;

	// 1. Arama Motoru: Veritabanından kullanıcıyı getiren alet.
	@Bean
	public UserDetailsService userDetailsService() {
		return username -> {

			return userRepository.findByEmail(username)
					.orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı kanzi!"));

		};
	}

	// 2. Kıyaslama Cihazı: Kullanıcıyı getiren motorla, şifreleyiciyi birleştiren
	// asıl sağlayıcı.
	@Bean
	public AuthenticationProvider authenticationProvider() {
		// 1. Artık UserDetailsService'i sınıfı oluştururken (parantez içinde)
		// veriyoruz!
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());

		// 2. Şifreleyiciyi set etmeye devam ediyoruz, çünkü koda göre
		// setPasswordEncoder metodu hala duruyor.
		authProvider.setPasswordEncoder(passwordEncoder());

		return authProvider;
	}

	// 3. Yönetici: Gelen giriş isteklerini (Login) yönetecek olan ana şef.
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	// 4. Şifreleme Aleti: Veritabanına şifreleri "123456" diye değil, karmaşık
	// (Hash) kaydetmek için.
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(); // Sektör standardı BCrypt algoritması
	}
}