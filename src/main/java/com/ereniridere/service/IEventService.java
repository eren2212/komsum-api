package com.ereniridere.service;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.event.DtoCreateEvent;
import com.ereniridere.dto.response.event.DtoEvent;

public interface IEventService {

	// 1. Etkinlik Oluştur
	public DtoEvent createEvent(Integer userId, DtoCreateEvent request);

	// 2. İlçe Bazlı Akış (Kendi ilçemdeki tüm etkinlikler)
	public Page<DtoEvent> getDistrictEvents(Integer userId, int pageNo, int pageSize);

	// 3. Katılım İşlemi (Katıl / Ayrıl Toggle)
	public String toggleParticipation(Integer userId, Integer eventId);

	// 4. Kaydetme İşlemi (Kaydet / Kaydedilenlerden Çıkar Toggle)
	public String toggleBookmark(Integer userId, Integer eventId);

	// 5. Benim Katıldığım Etkinlikler (Profil sekmesi için)
	public Page<DtoEvent> getMyJoinedEvents(Integer userId, int pageNo, int pageSize);

	// 6. Kaydettiğim Etkinlikler (Profil sekmesi için)
	public Page<DtoEvent> getMyBookmarkedEvents(Integer userId, int pageNo, int pageSize);

	// 7.Tekil Etkinlik Detayı Çekme
	public DtoEvent getEventById(Integer userId, Integer eventId);

	// 8. Kendi Oluşturduğum Etkinlikler
	public Page<DtoEvent> getMyEvents(Integer userId, int pageNo, int pageSize);
}