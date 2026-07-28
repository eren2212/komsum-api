package com.ereniridere.controller;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.roomio.DtoCreateRoomioProfile;
import com.ereniridere.dto.request.roomio.DtoSwipe;
import com.ereniridere.dto.request.roomio.DtoUpdateRoomioProfile;
import com.ereniridere.dto.response.roomio.DtoRoomioProfile;
import com.ereniridere.dto.response.roomio.DtoSwipeResult;
import com.ereniridere.entity.RootEntity;

public interface IRoomioController {

	public RootEntity<DtoRoomioProfile> createProfile(DtoCreateRoomioProfile request);

	public RootEntity<DtoRoomioProfile> updateProfile(DtoUpdateRoomioProfile request);

	public RootEntity<DtoRoomioProfile> getMyProfile();

	public RootEntity<Boolean> toggleActive();

	public RootEntity<Page<DtoRoomioProfile>> getCandidateFeed(int pageNo, int pageSize, Integer radius);

	public RootEntity<DtoSwipeResult> swipe(DtoSwipe request);

}
