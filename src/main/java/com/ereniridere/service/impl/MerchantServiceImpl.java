package com.ereniridere.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.Merchant.DtoCreateMerchant;
import com.ereniridere.dto.response.Merchant.DtoMerchant;
import com.ereniridere.entity.MerchantProfile;
import com.ereniridere.entity.User;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.MerchantProfileRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IMerchantService;

@Service
public class MerchantServiceImpl implements IMerchantService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MerchantProfileRepository merchantRepository;

	// Ensaf profil oluşturma
	@Override
	public DtoMerchant createMerchantProfile(Integer userId, DtoCreateMerchant request) {

		Optional<User> optionalUser = userRepository.findById(userId);

		if (optionalUser.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı!"));
		}

		User dbUser = optionalUser.get();

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Mahalleni seçmeden dükkan açamazsın!"));
		}

		Optional<MerchantProfile> existingProfile = merchantRepository.findByUserId(userId);

		if (existingProfile.isPresent()) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Zaten bir esnaf profiliniz var!"));
		}

		MerchantProfile newMerchantProfile = new MerchantProfile();

		BeanUtils.copyProperties(request, newMerchantProfile);

		newMerchantProfile.setUser(dbUser);
		newMerchantProfile.setNeighborhood(dbUser.getNeighborhood());
		newMerchantProfile.setVerified(false);

		MerchantProfile savedProfile = merchantRepository.save(newMerchantProfile);

		DtoMerchant dtoMerchant = new DtoMerchant();

		BeanUtils.copyProperties(savedProfile, dtoMerchant);
		dtoMerchant.setOwnerFirstName(dbUser.getFirstname());
		dtoMerchant.setOwnerLastName(dbUser.getLastname());

		return dtoMerchant;
	}

	// Mahalle esnaflarını listeleme
	@Override
	public List<DtoMerchant> getNeighborhoodMerchants(Integer userId) {

		Optional<User> optionalUser = userRepository.findById(userId);

		if (optionalUser.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı!"));
		}

		User dbUser = optionalUser.get();

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Mahalleni seçmeden esnafları göremezsin!"));
		}

		// Sadece adamın mahallesindeki ve ONAYLANMIŞ esnafları getir
		List<MerchantProfile> verifiedMerchants = merchantRepository
				.findByNeighborhoodIdAndIsVerifiedTrue(dbUser.getNeighborhood().getId());

		// Gelen listeyi DTO listesine çevir (Stream API ile çok şık bir şekilde)
		return verifiedMerchants.stream().map(merchant -> {
			DtoMerchant dto = new DtoMerchant();
			BeanUtils.copyProperties(merchant, dto);
			dto.setOwnerFirstName(merchant.getUser().getFirstname());
			dto.setOwnerLastName(merchant.getUser().getLastname());
			return dto;
		}).collect(Collectors.toList());
	}

	// Bir esnafın profilini getirme
	@Override
	public DtoMerchant getMerchantProfile(Integer userId) {

		Optional<User> optionalUser = userRepository.findById(userId);

		if (optionalUser.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı!"));
		}

		User dbUser = optionalUser.get();

		Optional<MerchantProfile> optionalMerchant = merchantRepository.findByUserId(userId);

		if (optionalMerchant.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST,
					"Bu kullanıcıya ait herhangi bir esnaf profili bulunamadı!"));
		}

		MerchantProfile merchantProfile = optionalMerchant.get();
		DtoMerchant dtoMerchant = new DtoMerchant();

		BeanUtils.copyProperties(merchantProfile, dtoMerchant);
		dtoMerchant.setOwnerFirstName(dbUser.getFirstname());
		dtoMerchant.setOwnerLastName(dbUser.getLastname());

		return dtoMerchant;
	}

}
