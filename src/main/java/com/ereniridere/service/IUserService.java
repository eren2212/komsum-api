package com.ereniridere.service;

import com.ereniridere.dto.request.user.DtoUserUpdate;
import com.ereniridere.dto.response.User.DtoNeighbour;
import com.ereniridere.dto.response.User.DtoUserProfile;

public interface IUserService {

	public DtoUserProfile getMyProfile(Integer id);

	public DtoNeighbour getNeighbourProfile(Integer id);

	public DtoUserProfile updateProfile(Integer id, DtoUserUpdate dtoUserUpdate);

}
