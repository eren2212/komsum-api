package com.ereniridere.controller;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.event.DtoCreateEvent;
import com.ereniridere.dto.response.event.DtoEvent;
import com.ereniridere.entity.RootEntity;

public interface IEventController {

	public RootEntity<DtoEvent> createEvent(DtoCreateEvent request);

	public RootEntity<Page<DtoEvent>> getDistrictEvents(int pageNo, int pageSize);

	public RootEntity<String> toggleParticipation(Integer eventId);

	public RootEntity<String> toggleBookmark(Integer eventId);

	public RootEntity<Page<DtoEvent>> getMyJoinedEvents(int pageNo, int pageSize);

	public RootEntity<Page<DtoEvent>> getMyBookmarkedEvents(int pageNo, int pageSize);

	public RootEntity<DtoEvent> getEventById(Integer eventId);

	public RootEntity<Page<DtoEvent>> getMyEvents(int pageNo, int pageSize);

}