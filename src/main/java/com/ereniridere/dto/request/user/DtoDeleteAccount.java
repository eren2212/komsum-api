package com.ereniridere.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Hesap silme isteği. Geri dönüşü olmayan bir işlem olduğu için kullanıcıdan
 * mevcut şifresini doğrulaması istenir (KVKK / güvenlik).
 */
@Data
public class DtoDeleteAccount {

	@NotBlank(message = "Hesabınızı silmek için şifrenizi girmeniz gerekiyor")
	private String password;
}
