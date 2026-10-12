package com.ereniridere.controller;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.response.User.DtoBlockedUser;
import com.ereniridere.entity.RootEntity;

public interface IUserBlockController {

	public RootEntity<Void> blockUser(Integer id);

	public RootEntity<Void> unblockUser(Integer id);

	public RootEntity<Page<DtoBlockedUser>> getMyBlockedUsers(int pageNo, int pageSize);
}
