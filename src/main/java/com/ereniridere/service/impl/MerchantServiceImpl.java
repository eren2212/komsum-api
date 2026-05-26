package com.ereniridere.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.Merchant.DtoCreateMerchant;
import com.ereniridere.dto.request.Merchant.DtoUpdateMerchant;
import com.ereniridere.dto.response.Merchant.DtoMerchant;
import com.ereniridere.entity.MerchantProfile;
import com.ereniridere.entity.Neighborhood;
import com.ereniridere.entity.User;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.handler.GlobalExceptionHandler;
import com.ereniridere.repository.MerchantProfileRepository;
import com.ereniridere.repository.NeighborhoodRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IMerchantService;
import com.ereniridere.util.GeoUtils;

@Service
public class MerchantServiceImpl implements IMerchantService {

	private final GlobalExceptionHandler globalExceptionHandler;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MerchantProfileRepository merchantRepository;

	@Autowired
	private NeighborhoodRepository neighborhoodRepository;

	MerchantServiceImpl(GlobalExceptionHandler globalExceptionHandler) {
		this.globalExceptionHandler = globalExceptionHandler;
	}

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

		// lat/lng -> JTS Point (SRID 4326). SPONSORED radius filtresi bu konumu kullanır.
		newMerchantProfile.setGeoLocation(GeoUtils.toPoint(request.getLatitude(), request.getLongitude()));

		MerchantProfile savedProfile = merchantRepository.save(newMerchantProfile);

		DtoMerchant dtoMerchant = new DtoMerchant();

		BeanUtils.copyProperties(savedProfile, dtoMerchant);
		dtoMerchant.setOwnerFirstName(dbUser.getFirstname());
		dtoMerchant.setOwnerLastName(dbUser.getLastname());
		dtoMerchant.setLatitude(GeoUtils.getLatitude(savedProfile.getGeoLocation()));
		dtoMerchant.setLongitude(GeoUtils.getLongitude(savedProfile.getGeoLocation()));

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
			dto.setLatitude(GeoUtils.getLatitude(merchant.getGeoLocation()));
			dto.setLongitude(GeoUtils.getLongitude(merchant.getGeoLocation()));
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
		dtoMerchant.setLatitude(GeoUtils.getLatitude(merchantProfile.getGeoLocation()));
		dtoMerchant.setLongitude(GeoUtils.getLongitude(merchantProfile.getGeoLocation()));

		return dtoMerchant;
	}

	// Bir esnaf işletme bilgilerini değiştirme /güncelleme
	@Override
	public DtoMerchant updateMerchantProfile(Integer userId, DtoUpdateMerchant request) {

		Optional<MerchantProfile> optional = merchantRepository.findByUserId(userId);

		if (optional.isEmpty()) {
			throw new BaseException(
					new ErrorMessage(MessageType.NO_RECORD_EXIST, "Güncelleyecek bir esnaf profilin yok!"));
		}

		MerchantProfile dbProfile = optional.get();

		if (request.getShopName() != null)
			dbProfile.setShopName(request.getShopName());
		if (request.getPhone() != null)
			dbProfile.setPhone(request.getPhone());
		if (request.getDescription() != null)
			dbProfile.setDescription(request.getDescription());
		if (request.getProfileImageUrl() != null)
			dbProfile.setProfileImageUrl(request.getProfileImageUrl());

		if (request.getNeighborhoodId() != null
				&& !dbProfile.getNeighborhood().getId().equals(request.getNeighborhoodId())) {
			Neighborhood newNeighborhood = neighborhoodRepository.findById(request.getNeighborhoodId())
					.orElseThrow(() -> new BaseException(
							new ErrorMessage(MessageType.NO_RECORD_EXIST, "Seçtiğiniz mahalle bulunamadı!")));
			dbProfile.setNeighborhood(newNeighborhood);
		}

		// Konum güncellemesi: lat/lng ikisi birden geldiyse Point'i yenile.
		// Sadece biri gelirse (eksik veri) mevcut konum korunur.
		if (request.getLatitude() != null && request.getLongitude() != null) {
			dbProfile.setGeoLocation(GeoUtils.toPoint(request.getLatitude(), request.getLongitude()));
		}

		MerchantProfile updatedProfile = merchantRepository.save(dbProfile);

		DtoMerchant dtoMerchant = new DtoMerchant();
		BeanUtils.copyProperties(updatedProfile, dtoMerchant);
		dtoMerchant.setOwnerFirstName(dbProfile.getUser().getFirstname());
		dtoMerchant.setOwnerLastName(dbProfile.getUser().getLastname());
		dtoMerchant.setLatitude(GeoUtils.getLatitude(updatedProfile.getGeoLocation()));
		dtoMerchant.setLongitude(GeoUtils.getLongitude(updatedProfile.getGeoLocation()));

		return dtoMerchant;
	}

	@Override
	public DtoMerchant getMyMerchantProfile(Integer userId) {

		Optional<MerchantProfile> optional = merchantRepository.findByUserId(userId);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Henüz bir esnaf profilin yok!"));
		}

		MerchantProfile dbProfile = optional.get();

		DtoMerchant dtoMerchant = new DtoMerchant();
		BeanUtils.copyProperties(dbProfile, dtoMerchant);
		dtoMerchant.setOwnerFirstName(dbProfile.getUser().getFirstname());
		dtoMerchant.setOwnerLastName(dbProfile.getUser().getLastname());
		dtoMerchant.setLatitude(GeoUtils.getLatitude(dbProfile.getGeoLocation()));
		dtoMerchant.setLongitude(GeoUtils.getLongitude(dbProfile.getGeoLocation()));

		return dtoMerchant;
	}

	@Override
	public boolean deleteMyMerchantProfile(Integer userId) {

		Optional<MerchantProfile> optional = merchantRepository.findByUserId(userId);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Henüz bir esnaf profilin yok!"));
		}

		MerchantProfile dbProfile = optional.get();

		merchantRepository.delete(dbProfile);

		return true;
	}

}
