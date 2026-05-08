package com.ereniridere.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.IEventController;
import com.ereniridere.dto.request.event.DtoCreateEvent;
import com.ereniridere.dto.response.event.DtoEvent;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.service.IEventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
public class EventControllerImpl extends BaseController implements IEventController {

	@Autowired
	private IEventService eventService;

	// 1. Etkinlik Oluştur
	@PostMapping("/create")
	@Override
	public RootEntity<DtoEvent> createEvent(@Valid @RequestBody DtoCreateEvent request) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(eventService.createEvent(userId, request));
	}

	// 2. İlçe Bazlı Akış (Kendi ilçemdeki etkinlikleri çeker)
	@GetMapping("/district")
	@Override
	public RootEntity<Page<DtoEvent>> getDistrictEvents(@RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "10") int pageSize) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(eventService.getDistrictEvents(userId, pageNo, pageSize));
	}

	// 3. Etkinliğe Katıl / Ayrıl (Toggle)
	@PostMapping("/{id}/participate")
	@Override
	public RootEntity<String> toggleParticipation(@PathVariable(value = "id") Integer eventId) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(eventService.toggleParticipation(userId, eventId));
	}

	// 4. Etkinliği Kaydet / Çıkar (Toggle)
	@PostMapping("/{id}/bookmark")
	@Override
	public RootEntity<String> toggleBookmark(@PathVariable(value = "id") Integer eventId) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(eventService.toggleBookmark(userId, eventId));
	}

	// 5. Profil Sekmesi: Katıldığım Etkinlikler
	@GetMapping("/my-joined")
	@Override
	public RootEntity<Page<DtoEvent>> getMyJoinedEvents(@RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "10") int pageSize) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(eventService.getMyJoinedEvents(userId, pageNo, pageSize));
	}

	// 6. Profil Sekmesi: Kaydettiğim Etkinlikler
	@GetMapping("/my-bookmarked")
	@Override
	public RootEntity<Page<DtoEvent>> getMyBookmarkedEvents(@RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "10") int pageSize) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(eventService.getMyBookmarkedEvents(userId, pageNo, pageSize));
	}

	// 7. Tekil Etkinlik Detayı (GET /api/events/{id})
	@GetMapping("/{id}")
	@Override
	public RootEntity<DtoEvent> getEventById(@PathVariable(value = "id") Integer eventId) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(eventService.getEventById(userId, eventId));
	}

// 8. Kendi Oluşturduğum Etkinlikler (GET /api/events/my-events)
	@GetMapping("/my-events")
	@Override
	public RootEntity<Page<DtoEvent>> getMyEvents(@RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "10") int pageSize) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(eventService.getMyEvents(userId, pageNo, pageSize));
	}

}