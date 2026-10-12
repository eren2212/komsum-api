package com.ereniridere.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.IUserBlockController;
import com.ereniridere.dto.response.User.DtoBlockedUser;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.service.IUserBlockService;

@RestController
@RequestMapping("/api/users")
public class UserBlockControllerImpl extends BaseController implements IUserBlockController {

	@Autowired
	private IUserBlockService userBlockService;

	// Kullanıcıyı engelle
	@PostMapping("/{id}/block")
	@Override
	public RootEntity<Void> blockUser(@PathVariable(name = "id") Integer id) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		userBlockService.blockUser(currentUser.getId(), id);
		return ok(null);
	}

	// Engeli kaldır
	@DeleteMapping("/{id}/block")
	@Override
	public RootEntity<Void> unblockUser(@PathVariable(name = "id") Integer id) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		userBlockService.unblockUser(currentUser.getId(), id);
		return ok(null);
	}

	// Engellediğim kullanıcılar
	@GetMapping("/me/blocks")
	@Override
	public RootEntity<Page<DtoBlockedUser>> getMyBlockedUsers(@RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "20") int pageSize) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		return ok(userBlockService.getMyBlockedUsers(currentUser.getId(), pageNo, pageSize));
	}
}
