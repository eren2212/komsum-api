package com.ereniridere.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.response.User.DtoBlockedUser;
import com.ereniridere.entity.User;
import com.ereniridere.entity.UserBlock;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.UserBlockRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IEmailService;
import com.ereniridere.service.IUserBlockService;

@Service
public class UserBlockServiceImpl implements IUserBlockService {

	@Autowired
	private UserBlockRepository userBlockRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private IEmailService emailService;

	@Override
	public void blockUser(Integer blockerId, Integer blockedId) {

		if (blockerId.equals(blockedId)) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Kendini engelleyemezsin"));
		}

		User blocker = userRepository.findById(blockerId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));
		User blocked = userRepository.findById(blockedId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Engellenecek kullanıcı bulunamadı")));

		// İdempotent: zaten engelliyse hata vermeden çık (çift tıklama vb.).
		if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
			return;
		}

		userBlockRepository.save(UserBlock.builder().blocker(blocker).blocked(blocked).build());

		// Apple 1.2: engelleme geliştiriciye de bildirilmeli (moderasyon için).
		emailService.sendModerationAlert("Kullanıcı engellendi",
				"Engelleyen: #" + blocker.getId() + " " + blocker.getFirstname() + " " + blocker.getLastname()
						+ "\nEngellenen: #" + blocked.getId() + " " + blocked.getFirstname() + " "
						+ blocked.getLastname());
	}

	@Override
	public void unblockUser(Integer blockerId, Integer blockedId) {
		userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
	}

	@Override
	public Page<DtoBlockedUser> getMyBlockedUsers(Integer blockerId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return userBlockRepository.findByBlockerIdWithBlocked(blockerId, pageable).map(this::toDto);
	}

	@Override
	public boolean isBlockedBetween(Integer a, Integer b) {
		return userBlockRepository.existsBlockBetween(a, b);
	}

	@Override
	public boolean hasBlocked(Integer blockerId, Integer blockedId) {
		return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
	}

	private DtoBlockedUser toDto(UserBlock block) {
		User blocked = block.getBlocked();
		DtoBlockedUser dto = new DtoBlockedUser();
		dto.setId(blocked.getId());
		dto.setFirstname(blocked.getFirstname());
		dto.setLastname(blocked.getLastname());
		dto.setAvatarUrl(blocked.getAvatarUrl());
		dto.setBlockedAt(block.getCreatedAt());
		return dto;
	}
}
