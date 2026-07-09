package com.ereniridere.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.serviceprovider.DtoCreateServiceProvider;
import com.ereniridere.dto.request.serviceprovider.DtoUpdateServiceProvider;
import com.ereniridere.dto.response.serviceprovider.DtoServiceProvider;
import com.ereniridere.entity.Neighborhood;
import com.ereniridere.entity.ServiceProviderProfile;
import com.ereniridere.entity.User;
import com.ereniridere.entity.enums.ServiceCategory;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.NeighborhoodRepository;
import com.ereniridere.repository.ServiceProviderProfileRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IServiceProviderService;
import com.ereniridere.util.GeoUtils;

@Service
public class ServiceProviderServiceImpl implements IServiceProviderService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ServiceProviderProfileRepository serviceProviderRepository;

	@Autowired
	private NeighborhoodRepository neighborhoodRepository;

	// Usta profili oluşturma
	@Override
	public DtoServiceProvider createServiceProviderProfile(Integer userId, DtoCreateServiceProvider request) {

		Optional<User> optionalUser = userRepository.findById(userId);

		if (optionalUser.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı!"));
		}

		User dbUser = optionalUser.get();

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Mahalleni seçmeden usta profili açamazsın!"));
		}

		Optional<ServiceProviderProfile> existingProfile = serviceProviderRepository.findByUserId(userId);

		if (existingProfile.isPresent()) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Zaten bir usta profiliniz var!"));
		}

		ServiceProviderProfile newProfile = new ServiceProviderProfile();

		// available'ı elle yöneteceğiz (gelmezse true). Diğer eşleşen alanlar kopyalanır.
		BeanUtils.copyProperties(request, newProfile, "available");

		newProfile.setAvailable(request.getAvailable() == null ? true : request.getAvailable());
		newProfile.setUser(dbUser);
		newProfile.setNeighborhood(dbUser.getNeighborhood());
		newProfile.setVerified(false);

		// lat/lng -> JTS Point (SRID 4326)
		newProfile.setGeoLocation(GeoUtils.toPoint(request.getLatitude(), request.getLongitude()));

		ServiceProviderProfile savedProfile = serviceProviderRepository.save(newProfile);

		return toDto(savedProfile, dbUser);
	}

	// Mahalledeki onaylı ustaları listeleme (opsiyonel kategori filtresi)
	@Override
	public List<DtoServiceProvider> getNeighborhoodServiceProviders(Integer userId, ServiceCategory category) {

		Optional<User> optionalUser = userRepository.findById(userId);

		if (optionalUser.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı!"));
		}

		User dbUser = optionalUser.get();

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Mahalleni seçmeden ustaları göremezsin!"));
		}

		Integer neighborhoodId = dbUser.getNeighborhood().getId();

		List<ServiceProviderProfile> providers = (category == null)
				? serviceProviderRepository.findByNeighborhoodIdAndIsVerifiedTrue(neighborhoodId)
				: serviceProviderRepository.findByNeighborhoodIdAndCategoryAndIsVerifiedTrue(neighborhoodId, category);

		return providers.stream()
				.map(provider -> toDto(provider, provider.getUser()))
				.collect(Collectors.toList());
	}

	// Konuma bağlı (km/radius) listeleme — iki-adımlı spatial sorgu (Event /nearby deseni)
	@Override
	public Page<DtoServiceProvider> getNearbyServiceProviders(Integer userId, Double lat, Double lng, Integer radius,
			ServiceCategory category, int pageNo, int pageSize) {

		Pageable pageable = PageRequest.of(pageNo, pageSize);

		// Konum gelmediyse geriye-uyumlu davranış: mahalle bazlı akışa düş.
		if (lat == null || lng == null) {
			List<DtoServiceProvider> fallback = getNeighborhoodServiceProviders(userId, category);
			return new PageImpl<>(fallback, pageable, fallback.size());
		}

		// Negatif/sıfır radius gelirse mantıklı bir varsayılana çek (10 km)
		int radiusMeters = (radius == null || radius <= 0) ? 10000 : radius;

		// ADIM 1: Spatial index ile radius içindeki ID sayfasını çek (en yakın en üstte)
		Page<Integer> idPage = (category == null)
				? serviceProviderRepository.findNearbyServiceProviderIds(lat, lng, radiusMeters, pageable)
				: serviceProviderRepository.findNearbyServiceProviderIdsByCategory(lat, lng, radiusMeters,
						category.name(), pageable);

		if (idPage.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
		}

		List<Integer> ids = idPage.getContent();

		// ADIM 2: ID'lerden JOIN FETCH ile hidrate et (N+1 yok)
		Map<Integer, ServiceProviderProfile> byId = serviceProviderRepository.findAllByIdInWithFetch(ids).stream()
				.collect(Collectors.toMap(ServiceProviderProfile::getId, Function.identity()));

		// Native sorgunun yakınlık sıralamasını koruyarak DTO listesi üret
		List<DtoServiceProvider> dtos = ids.stream().map(byId::get).filter(Objects::nonNull)
				.map(profile -> toDto(profile, profile.getUser())).collect(Collectors.toList());

		return new PageImpl<>(dtos, pageable, idPage.getTotalElements());
	}

	// Belirli bir kullanıcının usta profilini getirme
	@Override
	public DtoServiceProvider getServiceProviderProfile(Integer userId) {

		Optional<User> optionalUser = userRepository.findById(userId);

		if (optionalUser.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı!"));
		}

		User dbUser = optionalUser.get();

		Optional<ServiceProviderProfile> optional = serviceProviderRepository.findByUserId(userId);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST,
					"Bu kullanıcıya ait herhangi bir usta profili bulunamadı!"));
		}

		return toDto(optional.get(), dbUser);
	}

	@Override
	public DtoServiceProvider getMyServiceProviderProfile(Integer userId) {

		Optional<ServiceProviderProfile> optional = serviceProviderRepository.findByUserId(userId);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Henüz bir usta profilin yok!"));
		}

		ServiceProviderProfile dbProfile = optional.get();

		return toDto(dbProfile, dbProfile.getUser());
	}

	// Usta profilini güncelleme
	@Override
	public DtoServiceProvider updateServiceProviderProfile(Integer userId, DtoUpdateServiceProvider request) {

		Optional<ServiceProviderProfile> optional = serviceProviderRepository.findByUserId(userId);

		if (optional.isEmpty()) {
			throw new BaseException(
					new ErrorMessage(MessageType.NO_RECORD_EXIST, "Güncelleyecek bir usta profilin yok!"));
		}

		ServiceProviderProfile dbProfile = optional.get();

		if (request.getTitle() != null)
			dbProfile.setTitle(request.getTitle());
		if (request.getCategory() != null)
			dbProfile.setCategory(request.getCategory());
		if (request.getPhone() != null)
			dbProfile.setPhone(request.getPhone());
		if (request.getDescription() != null)
			dbProfile.setDescription(request.getDescription());
		if (request.getExperienceYears() != null)
			dbProfile.setExperienceYears(request.getExperienceYears());
		if (request.getPriceInfo() != null)
			dbProfile.setPriceInfo(request.getPriceInfo());
		if (request.getAvailable() != null)
			dbProfile.setAvailable(request.getAvailable());
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
		if (request.getLatitude() != null && request.getLongitude() != null) {
			dbProfile.setGeoLocation(GeoUtils.toPoint(request.getLatitude(), request.getLongitude()));
		}

		ServiceProviderProfile updatedProfile = serviceProviderRepository.save(dbProfile);

		return toDto(updatedProfile, dbProfile.getUser());
	}

	@Override
	public boolean deleteMyServiceProviderProfile(Integer userId) {

		Optional<ServiceProviderProfile> optional = serviceProviderRepository.findByUserId(userId);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Henüz bir usta profilin yok!"));
		}

		serviceProviderRepository.delete(optional.get());

		return true;
	}

	// Entity -> DTO dönüşümü (owner adı + Point'ten lat/lng) tek noktada.
	private DtoServiceProvider toDto(ServiceProviderProfile profile, User owner) {
		DtoServiceProvider dto = new DtoServiceProvider();
		BeanUtils.copyProperties(profile, dto);
		dto.setUserId(owner.getId());
		dto.setOwnerFirstName(owner.getFirstname());
		dto.setOwnerLastName(owner.getLastname());
		dto.setLatitude(GeoUtils.getLatitude(profile.getGeoLocation()));
		dto.setLongitude(GeoUtils.getLongitude(profile.getGeoLocation()));
		return dto;
	}

}
