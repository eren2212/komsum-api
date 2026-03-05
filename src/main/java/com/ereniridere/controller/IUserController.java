package com.ereniridere.controller;

import com.ereniridere.dto.request.user.DtoUserPassword;
import com.ereniridere.dto.request.user.DtoUserUpdate;
import com.ereniridere.dto.response.User.DtoNeighbour;
import com.ereniridere.dto.response.User.DtoUserProfile;
import com.ereniridere.entity.RootEntity;

public interface IUserController {

	public RootEntity<DtoUserProfile> getMyProfile();

	public RootEntity<DtoNeighbour> getNeighbourProfile(Integer id);

	public RootEntity<DtoUserProfile> updateProfile(DtoUserUpdate dtoUserUpdate);

	public RootEntity<Boolean> updatePassword(DtoUserPassword dtoUserPassword);
}
