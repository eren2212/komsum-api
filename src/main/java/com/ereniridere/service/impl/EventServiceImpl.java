package com.ereniridere.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.event.DtoCreateEvent;
import com.ereniridere.dto.response.event.DtoEvent;
import com.ereniridere.entity.Event;
import com.ereniridere.entity.EventBookmark;
import com.ereniridere.entity.EventParticipant;
import com.ereniridere.entity.User;
import com.ereniridere.event.EventCreatedEvent;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.EventBookmarkRepository;
import com.ereniridere.repository.EventParticipantRepository;
import com.ereniridere.repository.EventRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IEventService;
import com.ereniridere.util.GeoUtils;

@Service
public class EventServiceImpl implements IEventService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private EventParticipantRepository eventParticipantRepository;

	@Autowired
	private EventBookmarkRepository eventBookmarkRepository;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Override
	public DtoEvent createEvent(Integer userId, DtoCreateEvent request) {
		User dbUser = userRepository.findById(userId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(
					new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Bir mahalleye kayıt olmadan etkinlik açamazsın!"));
		}

		Event newEvent = new Event();
		// title, description, imageUrl, category, eventDate, location(mekân adı), priceText
		// kopyalanır. lat/lng entity'de olmadığı için BeanUtils onları atlar; Point'i elle set ederiz.
		BeanUtils.copyProperties(request, newEvent);

		newEvent.setAuthor(dbUser);
		newEvent.setNeighborhood(dbUser.getNeighborhood());

		// lat/lng -> JTS Point (SRID 4326) dönüşümü (DB'ye geometry olarak gider)
		newEvent.setGeoLocation(GeoUtils.toPoint(request.getLatitude(), request.getLongitude()));

		Event savedEvent = eventRepository.save(newEvent);

		// Async bildirim akışını tetikle (ilçedeki diğer kullanıcılara FCM + inbox)
		eventPublisher.publishEvent(new EventCreatedEvent(savedEvent.getId()));

		return convertToDto(savedEvent, userId);
	}

	@Override
	public Page<DtoEvent> getDistrictEvents(Integer userId, int pageNo, int pageSize) {
		User dbUser = userRepository.findById(userId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Mahalle bilgisi bulunamadı!"));
		}

		// 🚨 SİHİRLİ İLÇE MANTIĞIMIZ: Adamın mahallesinden İLÇEYİ çekiyoruz
		String district = dbUser.getNeighborhood().getDistrict();

		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<Event> events = eventRepository.findEventsByDistrict(district, pageable);

		return events.map(event -> convertToDto(event, userId));
	}

	// 2b. 🚨 YAKINLIK BAZLI AKIŞ (iki-adımlı spatial sorgu) 🚨
	@Override
	public Page<DtoEvent> getNearbyEvents(Integer userId, Double lat, Double lng, Integer radius, int pageNo,
			int pageSize) {

		// Konum gelmediyse geriye-uyumlu davranış: eski ilçe bazlı akışa düş.
		if (lat == null || lng == null) {
			return getDistrictEvents(userId, pageNo, pageSize);
		}

		// Negatif/sıfır radius gelirse mantıklı bir varsayılana çek (10 km)
		int radiusMeters = (radius == null || radius <= 0) ? 10000 : radius;

		Pageable pageable = PageRequest.of(pageNo, pageSize);

		// ADIM 1: Spatial index ile radius içindeki ID sayfasını çek (sıralı + count)
		Page<Integer> idPage = eventRepository.findNearbyEventIds(lat, lng, radiusMeters, pageable);

		if (idPage.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
		}

		List<Integer> ids = idPage.getContent();

		// ADIM 2: ID'lerden JOIN FETCH ile hidrate et (N+1 yok)
		Map<Integer, Event> byId = eventRepository.findAllByIdInWithFetch(ids).stream()
				.collect(Collectors.toMap(Event::getId, Function.identity()));

		// Native sorgunun sıralamasını (event_date ASC) koruyarak DTO listesi üret
		List<DtoEvent> dtos = ids.stream().map(byId::get).filter(java.util.Objects::nonNull)
				.map(event -> convertToDto(event, userId)).collect(Collectors.toList());

		return new PageImpl<>(dtos, pageable, idPage.getTotalElements());
	}

	@Override
	public String toggleParticipation(Integer userId, Integer eventId) {
		Event event = eventRepository.findById(eventId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Etkinlik bulunamadı")));

		if (!event.isActive()) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "İptal edilmiş bir etkinliğe katılamazsın!"));
		}

		Optional<EventParticipant> existingParticipant = eventParticipantRepository.findByEventIdAndUserId(eventId,
				userId);

		if (existingParticipant.isPresent()) {
			eventParticipantRepository.delete(existingParticipant.get());
			return "Etkinlikten ayrıldın.";
		} else {
			User user = userRepository.findById(userId).get();
			EventParticipant newParticipant = new EventParticipant();
			newParticipant.setEvent(event);
			newParticipant.setUser(user);
			eventParticipantRepository.save(newParticipant);
			return "Etkinliğe katıldın!";
		}
	}

	@Override
	public String toggleBookmark(Integer userId, Integer eventId) {
		Event event = eventRepository.findById(eventId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Etkinlik bulunamadı")));

		Optional<EventBookmark> existingBookmark = eventBookmarkRepository.findByEventIdAndUserId(eventId, userId);

		if (existingBookmark.isPresent()) {
			eventBookmarkRepository.delete(existingBookmark.get());
			return "Etkinlik kaydedilenlerden çıkarıldı.";
		} else {
			User user = userRepository.findById(userId).get();
			EventBookmark newBookmark = new EventBookmark();
			newBookmark.setEvent(event);
			newBookmark.setUser(user);
			eventBookmarkRepository.save(newBookmark);
			return "Etkinlik kaydedildi!";
		}
	}

	// 5. Profil Sekmesi: Katıldığım Etkinlikler
	@Override
	public Page<DtoEvent> getMyJoinedEvents(Integer userId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<EventParticipant> participants = eventParticipantRepository.findByUserIdOrderByJoinedAtDesc(userId,
				pageable);

		// EventParticipant objesinin içindeki Event'i alıp DTO'ya çeviriyoruz
		return participants.map(participant -> convertToDto(participant.getEvent(), userId));
	}

	// 6. Profil Sekmesi: Kaydettiğim Etkinlikler
	@Override
	public Page<DtoEvent> getMyBookmarkedEvents(Integer userId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<EventBookmark> bookmarks = eventBookmarkRepository.findByUserIdOrderBySavedAtDesc(userId, pageable);

		// EventBookmark objesinin içindeki Event'i alıp DTO'ya çeviriyoruz
		return bookmarks.map(bookmark -> convertToDto(bookmark.getEvent(), userId));
	}

	// 7.Event Detay
	@Override
	public DtoEvent getEventById(Integer userId, Integer eventId) {

		// 1. Etkinliği veritabanından bul
		Event event = eventRepository.findById(eventId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Etkinlik bulunamadı")));

		// 2. Güvenlik: Silinmiş veya iptal edilmiş bir etkinliğin detayına girilemez
		if (!event.isActive()) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Bu etkinlik iptal edilmiş veya silinmiş!"));
		}

		// 3. Ekrana basılacak DTO'ya çevir (Katılımcı sayısı, katıldım mı vb. bilgiler
		// otomatik dolacak)
		return convertToDto(event, userId);
	}

	// 8. Profil Sekmesi: Kendi Oluşturduğum Etkinlikler
	@Override
	public Page<DtoEvent> getMyEvents(Integer userId, int pageNo, int pageSize) {

		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<Event> myEvents = eventRepository.findByAuthorId(userId, pageable);

		// Yine sihirli convertToDto metodumuzu kullanıyoruz!
		return myEvents.map(event -> convertToDto(event, userId));
	}

	// 🚨 SENIOR DOKUNUŞU: DTO Dönüşüm Metodu (DRY Prensibi)
	private DtoEvent convertToDto(Event event, Integer currentUserId) {
		DtoEvent dto = new DtoEvent();
		// id, title, description, imageUrl, eventDate, location(mekân adı), priceText kopyalanır.
		BeanUtils.copyProperties(event, dto);

		// Point -> lat/lng çıkarımı (X=lng, Y=lat). Konum girilmemişse null kalır.
		dto.setLatitude(GeoUtils.getLatitude(event.getGeoLocation()));
		dto.setLongitude(GeoUtils.getLongitude(event.getGeoLocation()));
		dto.setCategory(event.getCategory());
		dto.setAuthorId(event.getAuthor().getId());
		dto.setAuthorFirstName(event.getAuthor().getFirstname());
		dto.setAuthorLastName(event.getAuthor().getLastname());
		dto.setNeighborhoodName(event.getNeighborhood().getName());

		dto.setParticipantCount(event.getParticipantCount() != null ? event.getParticipantCount() : 0);

		// Ben bu etkinliğe katıldım mı?
		boolean isJoined = eventParticipantRepository.existsByEventIdAndUserId(event.getId(), currentUserId);
		dto.setJoinedByMe(isJoined);

		// Ben bu etkinliği kaydettim mi?
		boolean isBookmarked = eventBookmarkRepository.existsByEventIdAndUserId(event.getId(), currentUserId);
		dto.setBookmarkedByMe(isBookmarked);

		return dto;
	}
}