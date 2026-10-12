package com.ereniridere.service;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.response.User.DtoBlockedUser;

public interface IUserBlockService {

	public void blockUser(Integer blockerId, Integer blockedId);

	public void unblockUser(Integer blockerId, Integer blockedId);

	public Page<DtoBlockedUser> getMyBlockedUsers(Integer blockerId, int pageNo, int pageSize);

	// İki yönlü: a b'yi ya da b a'yı engellemişse true.
	public boolean isBlockedBetween(Integer a, Integer b);

	// Sadece a'nın b'yi engelleyip engellemediği (profil ekranındaki buton için).
	public boolean hasBlocked(Integer blockerId, Integer blockedId);
}
