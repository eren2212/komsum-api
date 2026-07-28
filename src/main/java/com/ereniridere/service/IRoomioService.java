package com.ereniridere.service;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.roomio.DtoCreateRoomioProfile;
import com.ereniridere.dto.request.roomio.DtoSwipe;
import com.ereniridere.dto.request.roomio.DtoUpdateRoomioProfile;
import com.ereniridere.dto.response.roomio.DtoRoomioProfile;
import com.ereniridere.dto.response.roomio.DtoSwipeResult;

public interface IRoomioService {

	// 1. Roomio profili oluştur
	public DtoRoomioProfile createProfile(Integer userId, DtoCreateRoomioProfile request);

	// 2. Roomio profilini güncelle
	public DtoRoomioProfile updateProfile(Integer userId, DtoUpdateRoomioProfile request);

	// 3. Kendi Roomio profilini getir
	public DtoRoomioProfile getMyProfile(Integer userId);

	// 4. Profili aç/kapat (aday akışından gizle/göster)
	public boolean toggleActive(Integer userId);

	// 5. Yakındaki adayları getir (yarıçap bazlı, sayfalamalı)
	public Page<DtoRoomioProfile> getCandidateFeed(Integer userId, int pageNo, int pageSize, Integer radiusOverride);

	// 6. Kaydırma işlemi (LIKE/PASS) — karşılıklı LIKE varsa eşleşme + sohbet odası açar
	public DtoSwipeResult swipe(Integer swiperId, DtoSwipe request);
}
