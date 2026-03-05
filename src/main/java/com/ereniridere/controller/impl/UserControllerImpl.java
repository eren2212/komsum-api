package com.ereniridere.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.IUserController;
import com.ereniridere.dto.request.user.DtoUserPassword;
import com.ereniridere.dto.request.user.DtoUserUpdate;
import com.ereniridere.dto.response.User.DtoNeighbour;
import com.ereniridere.dto.response.User.DtoUserProfile;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.service.IUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserControllerImpl extends BaseController implements IUserController {

	@Autowired
	private IUserService userService;

	// Kendi profil görüntüleme
	@GetMapping(path = "/me")
	@Override
	public RootEntity<DtoUserProfile> getMyProfile() {

		// 1. Spring'in kasasından o an giriş yapmış (Token'ı doğrulanmış) adamı
		// alıyoruz
		// ve kendi User sınıfımıza çeviriyoruz (Casting).
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		// 2. Adamın ID'sini al!
		Integer userId = currentUser.getId();
		return ok(userService.getMyProfile(userId));
	}

	// İd si olan kullancının profili görüntüleme
	@GetMapping(path = "/{id}")
	@Override
	public RootEntity<DtoNeighbour> getNeighbourProfile(@PathVariable(name = "id") Integer id) {

		return ok(userService.getNeighbourProfile(id));
	}

	// Profil güncelleme
	@PatchMapping(path = "/me")
	@Override
	public RootEntity<DtoUserProfile> updateProfile(@RequestBody @Valid DtoUserUpdate dtoUserUpdate) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Integer userId = currentUser.getId();

		return ok(userService.updateProfile(userId, dtoUserUpdate));
	}

	// Şifre güncelleme
	@PatchMapping(path = "/me/update-password")
	@Override
	public RootEntity<Boolean> updatePassword(@Valid @RequestBody DtoUserPassword dtoUserPassword) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Integer userId = currentUser.getId();
		return ok(userService.updatePassword(userId, dtoUserPassword));
	}

}
