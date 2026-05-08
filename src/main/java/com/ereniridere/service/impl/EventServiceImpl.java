package com.ereniridere.service.impl;

import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.event.DtoCreateEvent;
import com.ereniridere.dto.response.event.DtoEvent;
import com.ereniridere.entity.Event;
import com.ereniridere.entity.EventBookmark;
import com.ereniridere.entity.EventParticipant;
import com.ereniridere.entity.User;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.EventBookmarkRepository;
import com.ereniridere.repository.EventParticipantRepository;
import com.ereniridere.repository.EventRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IEventService;

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

	@Override
	public DtoEvent createEvent(Integer userId, DtoCreateEvent request) {
		User dbUser = userRepository.findById(userId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(
					new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Bir mahalleye kayıt olmadan etkinlik açamazsın!"));
		}

		Event newEvent = new Event();
		BeanUtils.copyProperties(request, newEvent);

		newEvent.setAuthor(dbUser);
		newEvent.setNeighborhood(dbUser.getNeighborhood());

		Event savedEvent = eventRepository.save(newEvent);

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
		BeanUtils.copyProperties(event, dto);

		dto.setLatitude(event.getLatitude());
		dto.setLongitude(event.getLongitude());
		dto.setCategory(event.getCategory());
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