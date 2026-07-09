package com.ereniridere.service;

import org.springframework.web.multipart.MultipartFile;

public interface IStorageService {

	/**
	 * Bir resmi belirtilen klasör altına yükler ve istemcinin erişebileceği
	 * herkese açık (public) URL'i döndürür.
	 *
	 * @param file       yüklenecek resim dosyası
	 * @param folderName depo içindeki klasör (örn. "avatars/12")
	 * @return yüklenen dosyanın public URL'i
	 */
	String uploadImage(MultipartFile file, String folderName);
}
