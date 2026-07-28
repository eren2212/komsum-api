package com.ereniridere.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.message.DtoStartChat;
import com.ereniridere.dto.request.roomio.DtoCreateRoomioProfile;
import com.ereniridere.dto.request.roomio.DtoSwipe;
import com.ereniridere.dto.request.roomio.DtoUpdateRoomioProfile;
import com.ereniridere.dto.response.message.DtoChatRoom;
import com.ereniridere.dto.response.roomio.DtoRoomioMatch;
import com.ereniridere.dto.response.roomio.DtoRoomioMatchPush;
import com.ereniridere.dto.response.roomio.DtoRoomioProfile;
import com.ereniridere.dto.response.roomio.DtoSwipeResult;
import com.ereniridere.entity.ChatRoom;
import com.ereniridere.entity.RoomioMatch;
import com.ereniridere.entity.RoomioProfile;
import com.ereniridere.entity.RoomioProfilePhoto;
import com.ereniridere.entity.RoomioSwipe;
import com.ereniridere.entity.User;
import com.ereniridere.entity.enums.RoomioSwipeAction;
import com.ereniridere.event.RoomioMatchEvent;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.ChatRoomRepository;
import com.ereniridere.repository.MessageRepository;
import com.ereniridere.repository.RoomioMatchRepository;
import com.ereniridere.repository.RoomioProfileRepository;
import com.ereniridere.repository.RoomioSwipeRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IChatService;
import com.ereniridere.service.IRealtimeChatService;
import com.ereniridere.service.IRoomioService;
import com.ereniridere.util.GeoUtils;

@Service
public class RoomioServiceImpl implements IRoomioService {

	private static final int DEFAULT_RADIUS_METERS = 5000;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoomioProfileRepository roomioProfileRepository;

	@Autowired
	private RoomioSwipeRepository roomioSwipeRepository;

	@Autowired
	private RoomioMatchRepository roomioMatchRepository;

	@Autowired
	private IChatService chatService;

	@Autowired
	private IRealtimeChatService realtimeChatService;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Override
	public DtoRoomioProfile createProfile(Integer userId, DtoCreateRoomioProfile request) {

		User dbUser = userRepository.findById(userId)
				.orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı!")));

		if (roomioProfileRepository.findByUserId(userId).isPresent()) {
			throw new BaseException(new ErrorMessage(MessageType.ROOMIO_PROFILE_ALREADY_EXISTS, null));
		}

		RoomioProfile profile = new RoomioProfile();
		profile.setUser(dbUser);
		profile.setBio(request.getBio());
		profile.setAge(request.getAge());
		profile.setGender(request.getGender());
		profile.setNeighborhood(dbUser.getNeighborhood()); // sadece gösterim amaçlı
		profile.setActive(true);

		// lat/lng -> JTS Point (SRID 4326). Aday akışı radius filtresi bu konumu kullanır.
		profile.setGeoLocation(GeoUtils.toPoint(request.getLatitude(), request.getLongitude()));

		attachPhotos(profile, request.getPhotoUrls());

		RoomioProfile saved = roomioProfileRepository.save(profile);

		return toDto(saved);
	}

	@Override
	public DtoRoomioProfile updateProfile(Integer userId, DtoUpdateRoomioProfile request) {

		RoomioProfile dbProfile = roomioProfileRepository.findByUserId(userId)
				.orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.ROOMIO_PROFILE_NOT_FOUND, null)));

		if (request.getBio() != null)
			dbProfile.setBio(request.getBio());
		if (request.getAge() != null)
			dbProfile.setAge(request.getAge());
		if (request.getGender() != null)
			dbProfile.setGender(request.getGender());

		// Konum güncellemesi: lat/lng ikisi birden geldiyse Point'i yenile.
		if (request.getLatitude() != null && request.getLongitude() != null) {
			dbProfile.setGeoLocation(GeoUtils.toPoint(request.getLatitude(), request.getLongitude()));
		}

		if (request.getPhotoUrls() != null) {
			dbProfile.getPhotos().clear();
			attachPhotos(dbProfile, request.getPhotoUrls());
		}

		RoomioProfile updated = roomioProfileRepository.save(dbProfile);

		return toDto(updated);
	}

	@Override
	public DtoRoomioProfile getMyProfile(Integer userId) {
		RoomioProfile dbProfile = roomioProfileRepository.findByUserId(userId)
				.orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.ROOMIO_PROFILE_NOT_FOUND, null)));

		return toDto(dbProfile);
	}

	@Override
	public boolean toggleActive(Integer userId) {
		RoomioProfile dbProfile = roomioProfileRepository.findByUserId(userId)
				.orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.ROOMIO_PROFILE_NOT_FOUND, null)));

		dbProfile.setActive(!dbProfile.isActive());
		roomioProfileRepository.save(dbProfile);

		return dbProfile.isActive();
	}

	@Override
	public Page<DtoRoomioProfile> getCandidateFeed(Integer userId, int pageNo, int pageSize, Integer radiusOverride) {

		RoomioProfile myProfile = roomioProfileRepository.findByUserId(userId)
				.orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.ROOMIO_PROFILE_NOT_FOUND,
						"Aday görebilmek için önce Roomio profili oluşturmalısın!")));

		if (myProfile.getGeoLocation() == null) {
			throw new BaseException(new ErrorMessage(MessageType.ROOMIO_LOCATION_REQUIRED, null));
		}

		double lat = GeoUtils.getLatitude(myProfile.getGeoLocation());
		double lng = GeoUtils.getLongitude(myProfile.getGeoLocation());
		int radius = radiusOverride != null ? radiusOverride : DEFAULT_RADIUS_METERS;

		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<Integer> idPage = roomioProfileRepository.findNearbyCandidateIds(userId, lat, lng, radius, pageable);

		if (idPage.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
		}

		List<Integer> orderedIds = idPage.getContent();
		List<RoomioProfile> hydrated = roomioProfileRepository.findAllByIdInWithFetch(orderedIds);

		// Hidrasyon sorgusu sırayı garanti etmez — orijinal (mesafeye göre) sıraya göre yeniden sırala.
		Map<Integer, RoomioProfile> byId = hydrated.stream()
				.collect(Collectors.toMap(RoomioProfile::getId, p -> p));

		List<DtoRoomioProfile> content = orderedIds.stream()
				.map(byId::get)
				.filter(p -> p != null)
				.map(this::toDto)
				.collect(Collectors.toList());

		return new PageImpl<>(content, pageable, idPage.getTotalElements());
	}

	@Override
	public DtoSwipeResult swipe(Integer swiperId, DtoSwipe request) {

		Integer targetId = request.getTargetUserId();

		if (swiperId.equals(targetId)) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Kendini kaydıramazsın!"));
		}

		if (roomioSwipeRepository.findBySwiperIdAndSwipedId(swiperId, targetId).isPresent()) {
			throw new BaseException(new ErrorMessage(MessageType.ROOMIO_ALREADY_SWIPED, null));
		}

		User swiper = userRepository.findById(swiperId)
				.orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı!")));
		User target = userRepository.findById(targetId)
				.orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kaydırdığın kullanıcı bulunamadı!")));

		RoomioSwipe swipe = new RoomioSwipe();
		swipe.setSwiper(swiper);
		swipe.setSwiped(target);
		swipe.setAction(request.getAction());
		roomioSwipeRepository.save(swipe);

		DtoSwipeResult result = new DtoSwipeResult();
		result.setMatch(false);

		if (request.getAction() != RoomioSwipeAction.LIKE) {
			return result;
		}

		// Karşı taraf beni daha önce LIKE'lamış mı? (mutual match)
		boolean reverseLikeExists = roomioSwipeRepository.existsBySwiperIdAndSwipedIdAndAction(targetId, swiperId,
				RoomioSwipeAction.LIKE);

		if (!reverseLikeExists) {
			return result; // Karşı taraf henüz sıra bekleniyor
		}

		// Eşleşme zaten var mı? (savunma amaçlı idempotency kontrolü)
		Optional<RoomioMatch> existingMatch = roomioMatchRepository.findMatchBetweenUsers(swiperId, targetId);

		RoomioMatch match;
		DtoChatRoom room;
		if (existingMatch.isPresent()) {
			match = existingMatch.get();
			room = chatService.startChat(swiperId, buildStartChatRequest(targetId));
		} else {
			room = chatService.startChat(swiperId, buildStartChatRequest(targetId));

			match = new RoomioMatch();
			match.setUserA(swiper);
			match.setUserB(target);
			match.setChatRoomId(room.getId());
			match = roomioMatchRepository.save(match);

			eventPublisher.publishEvent(new RoomioMatchEvent(match.getId(), swiperId, targetId));
		}

		// Anlık teslim (SSE) — her iki tarafa da, karşı taraf ekranda ise beklemeden.
		sendMatchPush(match, swiperId, targetId);
		sendMatchPush(match, targetId, swiperId);

		result.setMatch(true);
		result.setMatchId(match.getId());
		result.setChatRoomId(room.getId());
		result.setMatchedUser(getProfileForUser(targetId));

		return result;
	}

	@Override
	public Page<DtoRoomioMatch> getMyMatches(Integer userId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<RoomioMatch> matches = roomioMatchRepository.findAllByParticipant(userId, pageable);

		return matches.map(match -> toDtoRoomioMatch(match, userId));
	}

	private DtoRoomioMatch toDtoRoomioMatch(RoomioMatch match, Integer currentUserId) {
		User other = match.getUserA().getId().equals(currentUserId) ? match.getUserB() : match.getUserA();

		DtoRoomioMatch dto = new DtoRoomioMatch();
		dto.setMatchId(match.getId());
		dto.setChatRoomId(match.getChatRoomId());
		dto.setOtherUserId(other.getId());
		dto.setOtherUserFirstName(other.getFirstname());
		dto.setOtherUserLastName(other.getLastname());
		dto.setOtherUserAvatarUrl(other.getAvatarUrl());
		dto.setMatchedAt(match.getCreatedAt());

		ChatRoom room = chatRoomRepository.findById(match.getChatRoomId()).orElse(null);
		if (room != null) {
			dto.setLastMessageContent(room.getLastMessageContent());
			dto.setLastMessageAt(room.getLastMessageAt());
			dto.setUnreadCount((int) messageRepository.countUnreadMessages(room.getId(), currentUserId));
		}

		return dto;
	}

	private void sendMatchPush(RoomioMatch match, Integer recipientId, Integer otherUserId) {
		User other = userRepository.findById(otherUserId).orElse(null);
		if (other == null) {
			return;
		}
		DtoRoomioMatchPush push = new DtoRoomioMatchPush();
		push.setMatchId(match.getId());
		push.setChatRoomId(match.getChatRoomId());
		push.setOtherUserId(other.getId());
		push.setOtherUserFirstName(other.getFirstname());
		push.setOtherUserLastName(other.getLastname());
		push.setOtherUserAvatarUrl(other.getAvatarUrl());
		realtimeChatService.sendRoomioMatchToUser(recipientId, push);
	}

	private DtoRoomioProfile getProfileForUser(Integer userId) {
		return roomioProfileRepository.findByUserId(userId).map(this::toDto).orElse(null);
	}

	private DtoStartChat buildStartChatRequest(Integer targetUserId) {
		DtoStartChat request = new DtoStartChat();
		request.setTargetUserId(targetUserId);
		return request;
	}

	private void attachPhotos(RoomioProfile profile, List<String> photoUrls) {
		if (photoUrls == null) {
			return;
		}
		for (String url : photoUrls) {
			RoomioProfilePhoto photo = new RoomioProfilePhoto();
			photo.setRoomioProfile(profile);
			photo.setPhotoUrl(url);
			profile.getPhotos().add(photo);
		}
	}

	private DtoRoomioProfile toDto(RoomioProfile profile) {
		DtoRoomioProfile dto = new DtoRoomioProfile();
		dto.setId(profile.getId());
		dto.setUserId(profile.getUser().getId());
		dto.setFirstName(profile.getUser().getFirstname());
		dto.setLastName(profile.getUser().getLastname());
		dto.setBio(profile.getBio());
		dto.setAge(profile.getAge());
		dto.setGender(profile.getGender());
		dto.setPhotoUrls(profile.getPhotos().stream().map(RoomioProfilePhoto::getPhotoUrl)
				.collect(Collectors.toCollection(ArrayList::new)));
		dto.setLatitude(GeoUtils.getLatitude(profile.getGeoLocation()));
		dto.setLongitude(GeoUtils.getLongitude(profile.getGeoLocation()));
		dto.setNeighborhoodName(profile.getNeighborhood() != null ? profile.getNeighborhood().getName() : null);
		dto.setActive(profile.isActive());
		return dto;
	}
}
