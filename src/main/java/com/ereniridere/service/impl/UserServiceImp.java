package com.ereniridere.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.dto.request.user.DtoDeleteAccount;
import com.ereniridere.dto.request.user.DtoUserPassword;
import com.ereniridere.dto.request.user.DtoUserUpdate;
import com.ereniridere.dto.response.User.DtoNeighborhood;
import com.ereniridere.dto.response.User.DtoNeighbour;
import com.ereniridere.dto.response.User.DtoUserProfile;
import com.ereniridere.entity.Neighborhood;
import com.ereniridere.entity.User;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.ChatRoomRepository;
import com.ereniridere.repository.CommentRepository;
import com.ereniridere.repository.EventBookmarkRepository;
import com.ereniridere.repository.EventParticipantRepository;
import com.ereniridere.repository.EventRepository;
import com.ereniridere.repository.MarketplaceListingRepository;
import com.ereniridere.repository.MerchantProfileRepository;
import com.ereniridere.repository.MessageRepository;
import com.ereniridere.repository.NeighborhoodRepository;
import com.ereniridere.repository.NotificationRepository;
import com.ereniridere.repository.PostLikeRepository;
import com.ereniridere.repository.PostRepository;
import com.ereniridere.repository.RefreshTokenRepository;
import com.ereniridere.repository.RoomioMatchRepository;
import com.ereniridere.repository.RoomioProfileRepository;
import com.ereniridere.repository.RoomioSwipeRepository;
import com.ereniridere.repository.ServiceProviderProfileRepository;
import com.ereniridere.repository.UserConsentRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.security.filter.JwtAuthenticationFilter;
import com.ereniridere.service.IUserService;

@Service
public class UserServiceImp implements IUserService {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NeighborhoodRepository neighborhoodRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	// Hesap silme için kullanıcıya bağlı tüm verileri temizleyen repository'ler
	@Autowired
	private NotificationRepository notificationRepository;
	@Autowired
	private UserConsentRepository userConsentRepository;
	@Autowired
	private ServiceProviderProfileRepository serviceProviderProfileRepository;
	@Autowired
	private MerchantProfileRepository merchantProfileRepository;
	@Autowired
	private EventParticipantRepository eventParticipantRepository;
	@Autowired
	private EventBookmarkRepository eventBookmarkRepository;
	@Autowired
	private EventRepository eventRepository;
	@Autowired
	private CommentRepository commentRepository;
	@Autowired
	private PostLikeRepository postLikeRepository;
	@Autowired
	private PostRepository postRepository;
	@Autowired
	private MarketplaceListingRepository marketplaceListingRepository;
	@Autowired
	private MessageRepository messageRepository;
	@Autowired
	private ChatRoomRepository chatRoomRepository;
	@Autowired
	private RoomioMatchRepository roomioMatchRepository;
	@Autowired
	private RoomioSwipeRepository roomioSwipeRepository;
	@Autowired
	private RoomioProfileRepository roomioProfileRepository;
	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	UserServiceImp(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	// Kendi Profil bilgilerini alma
	@Override
	public DtoUserProfile getMyProfile(Integer id) {

		Optional<User> optional = userRepository.findById(id);

		DtoUserProfile dtoUserProfileResponse = new DtoUserProfile();
		DtoNeighborhood dtoNeighborhood = new DtoNeighborhood();

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadi"));
		}

		User dbUser = optional.get();
		Neighborhood dbNeighborhood = optional.get().getNeighborhood();

		BeanUtils.copyProperties(dbUser, dtoUserProfileResponse);
		BeanUtils.copyProperties(dbNeighborhood, dtoNeighborhood);

		dtoUserProfileResponse.setNeighborhood(dtoNeighborhood);

		return dtoUserProfileResponse;
	}

	// Herhangi bir kişinin profilini alma
	@Override
	public DtoNeighbour getNeighbourProfile(Integer id) {

		Optional<User> optional = userRepository.findById(id);

		DtoNeighbour dtoNeighbour = new DtoNeighbour();
		DtoNeighborhood dtoNeighborhood = new DtoNeighborhood();

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadi"));
		}
		User dbUser = optional.get();
		Neighborhood dbNeighborhood = optional.get().getNeighborhood();

		BeanUtils.copyProperties(dbUser, dtoNeighbour);
		BeanUtils.copyProperties(dbNeighborhood, dtoNeighborhood);

		dtoNeighbour.setNeighborhood(dtoNeighborhood);

		return dtoNeighbour;
	}

	// İsim ,soyisim ve adres güncelleme
	// İsim, soyisim, adres, bio ve avatar güncelleme
	@Override
	public DtoUserProfile updateProfile(Integer id, DtoUserUpdate dtoUserUpdate) {

		Optional<User> optional = userRepository.findById(id);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı"));
		}

		User dbUser = optional.get();

		// 🚨 SENIOR DOKUNUŞU (NullPointerException Koruması):
		// Önce dtoUserUpdate içinden gelen değer null mu diye bakıyoruz ki patlamasın!
		if (dtoUserUpdate.getFirstname() != null && !dtoUserUpdate.getFirstname().trim().isEmpty()) {
			dbUser.setFirstname(dtoUserUpdate.getFirstname().trim());
		}

		if (dtoUserUpdate.getLastname() != null && !dtoUserUpdate.getLastname().trim().isEmpty()) {
			dbUser.setLastname(dtoUserUpdate.getLastname().trim());
		}

		// YENİ ALANLAR (Esnek Güncelleme)
		if (dtoUserUpdate.getBio() != null) {
			dbUser.setBio(dtoUserUpdate.getBio().trim());
		}

		if (dtoUserUpdate.getAvatarUrl() != null && !dtoUserUpdate.getAvatarUrl().trim().isEmpty()) {
			dbUser.setAvatarUrl(dtoUserUpdate.getAvatarUrl().trim());
		}

		// SENİN EFSANE MAHALLE GÜNCELLEME MANTIĞIN (Buna hiç dokunmuyorum, mükemmel)
		if (dtoUserUpdate.getNeighborhoodId() != null && (dbUser.getNeighborhood() == null
				|| !dbUser.getNeighborhood().getId().equals(dtoUserUpdate.getNeighborhoodId()))) {

			if (dbUser.getLastNeighborhoodChange() != null
					&& dbUser.getLastNeighborhoodChange().plusDays(180).isAfter(LocalDateTime.now())) {
				throw new BaseException(new ErrorMessage(MessageType.COOLDOWN_ACTIVE,
						"Mahalleniz yılda en fazla 2 kere değiştirilebiliyor!"));
			}

			Optional<Neighborhood> optional2 = neighborhoodRepository.findById(dtoUserUpdate.getNeighborhoodId());

			if (optional2.isEmpty()) {
				throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Mahalle bulunamadı"));
			}

			Neighborhood newNeighborhood = optional2.get();

			dbUser.setNeighborhood(newNeighborhood);
			dbUser.setLastNeighborhoodChange(LocalDateTime.now());
			dbUser.setVerifiedNeighbor(false);
		}

		userRepository.save(dbUser);

		return getMyProfile(id); // Güncel halini geri dön!
	}

	// Şifre güncelleme
	@Override
	public boolean updatePassword(Integer idInteger, DtoUserPassword dtoUserPassword) {
		Optional<User> optional = userRepository.findById(idInteger);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı"));
		}

		User dbUser = optional.get();

		if (!passwordEncoder.matches(dtoUserPassword.getOldPassword(), dbUser.getPassword())) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Mevcut şifrenizi yanlış girdiniz."));
		}

		if (!dtoUserPassword.getNewPassword().equals(dtoUserPassword.getConfirmNewPassword())) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED,
					"Yeni şifreler birbiriyle eşleşmiyor. Lütfen tekrar deneyin."));
		}

		// 4. Bütün güvenlik duvarlarını geçtik! Artık içimiz rahat bir şekilde şifreyi
		// kaydet
		dbUser.setPassword(passwordEncoder.encode(dtoUserPassword.getNewPassword()));
		userRepository.save(dbUser);

		return true; // İşlem başarılı!

	}

	// Hesap silme (KVKK + App Store/Google Play). Kullanıcıyı ve ona bağlı TÜM
	// verileri kalıcı olarak siler. Foreign key kısıtları için silme sırası
	// önemlidir: önce çocuk kayıtlar, en son kullanıcının kendisi. Tek transaction
	// içinde yapılır — herhangi bir adım patlarsa hiçbiri silinmez.
	@Override
	@Transactional
	public void deleteMyAccount(Integer userId, DtoDeleteAccount request) {

		User dbUser = userRepository.findById(userId)
				.orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));

		// Geri dönüşü olmayan işlem — şifre doğrulaması iste.
		if (!passwordEncoder.matches(request.getPassword(), dbUser.getPassword())) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Şifreniz hatalı."));
		}

		// 1) Bildirimler (alıcı veya aktör)
		notificationRepository.deleteAllByUserId(userId);
		// 2) KVKK / onay kayıtları
		userConsentRepository.deleteAllByUserId(userId);
		// 3) Profiller (esnaf / usta)
		serviceProviderProfileRepository.deleteAllByUserId(userId);
		merchantProfileRepository.deleteAllByUserId(userId);
		// 3b) Roomio (eşleşme → swipe → fotoğraf → profil sırasıyla, FK kısıtları için)
		roomioMatchRepository.deleteAllByParticipant(userId);
		roomioSwipeRepository.deleteAllBySwiperIdOrSwipedId(userId);
		roomioProfileRepository.deletePhotosByUserId(userId);
		roomioProfileRepository.deleteAllByUserId(userId);
		// 4) Etkinlik katılım/bookmark kayıtları (kendi + sildiği etkinliklere ait)
		eventParticipantRepository.deleteAllByUserIdOrAuthoredEvents(userId);
		eventBookmarkRepository.deleteAllByUserIdOrAuthoredEvents(userId);
		// 5) Etkinlikler
		eventRepository.deleteAllByAuthorId(userId);
		// 6) Yorum ve beğeniler (kendi + kendi postlarına gelenler) → sonra postlar
		commentRepository.deleteAllByAuthorIdOrAuthoredPosts(userId);
		postLikeRepository.deleteAllByUserIdOrAuthoredPosts(userId);
		postRepository.deleteAllByAuthorId(userId);
		// 7) Pazar yeri ilanları
		marketplaceListingRepository.deleteAllByUserId(userId);
		// 8) Mesajlar → sonra sohbet odaları
		messageRepository.deleteAllByUserChatRooms(userId);
		chatRoomRepository.deleteAllByParticipant(userId);
		// 9) Oturum kayıtları (user_id FK'sı kullanıcı silinmesini engellemesin)
		refreshTokenRepository.deleteAllByUserId(userId);
		// 10) En son: kullanıcının kendisi
		userRepository.delete(dbUser);
	}
}
