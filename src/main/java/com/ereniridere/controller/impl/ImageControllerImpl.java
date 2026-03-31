package com.ereniridere.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ereniridere.entity.RootEntity;
import com.ereniridere.service.impl.SupabaseStorageServiceImpl;

@RestController
@RequestMapping("/api/images")
class ImageControllerImpl extends BaseController {

	@Autowired
	private SupabaseStorageServiceImpl storageService;

	// Mobilden resmi yollarken artık URL'in sonuna klasör adını ekleyecek!
	// Örnek: POST /api/images/upload/merchants
	// Örnek: POST /api/images/upload/profiles
	// Örnek: POST /api/images/upload/posts
	@PostMapping("/upload/{folderName}")
	public RootEntity<String> uploadImage(@PathVariable String folderName, @RequestParam("file") MultipartFile file) {

		// Klasör adını ve dosyayı servise yolla
		return ok(storageService.uploadImage(file, folderName));
	}
}
