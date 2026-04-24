package com.ereniridere.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.service.impl.SupabaseStorageServiceImpl;

@RestController
@RequestMapping("/api/upload")
public class UploadControllerImpl extends BaseController {

	@Autowired
	private SupabaseStorageServiceImpl supabaseStorageService;

	/** POST /api/upload/avatar – Profil fotoğrafı yükle */
	@PostMapping("/avatar")
	public RootEntity<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String folder = "avatars/" + currentUser.getId();
		return ok(supabaseStorageService.uploadImage(file, folder));
	}

	/** POST /api/upload/post-image – Gönderi fotoğrafı yükle */
	@PostMapping("/post-image")
	public RootEntity<String> uploadPostImage(@RequestParam("file") MultipartFile file) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String folder = "post-images/" + currentUser.getId();
		return ok(supabaseStorageService.uploadImage(file, folder));
	}

	/** POST /api/upload/listing-image – Pazar yeri ilan fotoğrafı yükle */
	@PostMapping("/listing-image")
	public RootEntity<String> uploadListingImage(@RequestParam("file") MultipartFile file) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String folder = "listing-images/" + currentUser.getId();
		return ok(supabaseStorageService.uploadImage(file, folder));
	}
}
