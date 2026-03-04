package com.ereniridere.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.user.DtoUserUpdate;
import com.ereniridere.dto.response.User.DtoNeighborhood;
import com.ereniridere.dto.response.User.DtoNeighbour;
import com.ereniridere.dto.response.User.DtoUserProfile;
import com.ereniridere.entity.Neighborhood;
import com.ereniridere.entity.User;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.NeighborhoodRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IUserService;

@Service
public class UserServiceImp implements IUserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NeighborhoodRepository neighborhoodRepository;

	@Override
	public DtoUserProfile getMyProfile(Integer id) {

		Optional<User> optional = userRepository.findById(id);

		DtoUserProfile dtoUserProfileResponse = new DtoUserProfile();
		DtoNeighborhood dtoNeighborhood = new DtoNeighborhood();

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadi"));
		}

		User dbUser = optional.get();
		Neighborhood dbNeighborhood = optional.get().getNeighborhood();

		BeanUtils.copyProperties(dbUser, dtoUserProfileResponse);
		BeanUtils.copyProperties(dbNeighborhood, dtoNeighborhood);

		dtoUserProfileResponse.setNeighborhood(dtoNeighborhood);

		return dtoUserProfileResponse;
	}

	@Override
	public DtoNeighbour getNeighbourProfile(Integer id) {

		Optional<User> optional = userRepository.findById(id);

		DtoNeighbour dtoNeighbour = new DtoNeighbour();
		DtoNeighborhood dtoNeighborhood = new DtoNeighborhood();

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadi"));
		}
		User dbUser = optional.get();
		Neighborhood dbNeighborhood = optional.get().getNeighborhood();

		BeanUtils.copyProperties(dbUser, dtoNeighbour);
		BeanUtils.copyProperties(dbNeighborhood, dtoNeighborhood);

		dtoNeighbour.setNeighborhood(dtoNeighborhood);

		return dtoNeighbour;
	}

	@Override
	public DtoUserProfile updateProfile(Integer id, DtoUserUpdate dtoUserUpdate) {

		Optional<User> optional = userRepository.findById(id);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadi"));
		}

		User dbUser = optional.get();

		if (dbUser.getFirstname() != null && !dtoUserUpdate.getFirstname().trim().isEmpty()) {
			dbUser.setFirstname(dtoUserUpdate.getFirstname());
		}

		if (dbUser.getLastname() != null && !dtoUserUpdate.getLastname().trim().isEmpty()) {
			dbUser.setLastname(dtoUserUpdate.getLastname());
		}

		if (dtoUserUpdate.getNeighborhoodId() != null && (dbUser.getNeighborhood() == null
				|| !dbUser.getNeighborhood().getId().equals(dtoUserUpdate.getNeighborhoodId()))) {

			// Eğer daha önce mahalle değiştirmişse ve 6 ay geçmemişse, fırlat hatayı!
			if (dbUser.getLastNeighborhoodChange() != null
					&& dbUser.getLastNeighborhoodChange().plusDays(180).isAfter(LocalDateTime.now())) {
				throw new BaseException(new ErrorMessage(MessageType.COOLDOWN_ACTIVE,
						"Mahalleniz yılda en fazla 2 kere değiştirilebiliyor!"));
			}

			Optional<Neighborhood> optional2 = neighborhoodRepository.findById(dtoUserUpdate.getNeighborhoodId());

			if (optional2.isEmpty()) {
				throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Mahalle bulunamadı"));
			}

			Neighborhood newNeighborhood = optional2.get();

			dbUser.setNeighborhood(newNeighborhood);
			dbUser.setLastNeighborhoodChange(LocalDateTime.now());
			dbUser.setVerifiedNeighbor(false);

		}

		userRepository.save(dbUser);
		return getMyProfile(id);
	}

}
