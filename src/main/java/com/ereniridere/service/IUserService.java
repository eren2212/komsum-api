package com.ereniridere.service;

import com.ereniridere.dto.request.user.DtoDeleteAccount;
import com.ereniridere.dto.request.user.DtoUserPassword;
import com.ereniridere.dto.request.user.DtoUserUpdate;
import com.ereniridere.dto.response.User.DtoNeighbour;
import com.ereniridere.dto.response.User.DtoUserProfile;

public interface IUserService {

	public DtoUserProfile getMyProfile(Integer id);

	public DtoNeighbour getNeighbourProfile(Integer id);

	public DtoUserProfile updateProfile(Integer id, DtoUserUpdate dtoUserUpdate);

	public boolean updatePassword(Integer idInteger, DtoUserPassword dtoUserPassword);

	// Hesap silme (KVKK + App Store/Google Play zorunluluğu). Kullanıcının kendi
	// hesabını ve ona bağlı tüm verileri kalıcı olarak siler.
	public void deleteMyAccount(Integer userId, DtoDeleteAccount request);

}
