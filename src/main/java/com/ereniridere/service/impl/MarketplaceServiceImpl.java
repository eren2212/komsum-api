package com.ereniridere.service.impl;

import java.math.BigDecimal;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.market.DtoCreateListing;
import com.ereniridere.dto.response.market.DtoListing;
import com.ereniridere.entity.MarketplaceListing;
import com.ereniridere.entity.User;
import com.ereniridere.entity.enums.ListingStatus;
import com.ereniridere.entity.enums.ListingType;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.MarketplaceListingRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IMarketplaceService;

@Service

public class MarketplaceServiceImpl implements IMarketplaceService {

	@Autowired
	private MarketplaceListingRepository marketplaceRepository;

	@Autowired
	private UserRepository userRepository;

	// 1. İLAN OLUŞTURMA (Ürün Ekle Sayfası)
	public DtoListing createListing(Integer userId, DtoCreateListing request) {

		User dbUser = userRepository.findById(userId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED,
					"Mahalleni seçmeden pazar yerine ilan koyamazsın!"));
		}

		// SENIOR GUARD CLAUSE: İlan Türüne Göre Fiyat Kontrolü
		if (request.getType() == ListingType.FOR_SALE) {
			// Satılıksa fiyat zorunludur ve 0'dan büyük olmalıdır!
			if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
				throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED,
						"Satılık ilanlarda geçerli bir fiyat girmek zorundasın kanzi!"));
			}
		} else if (request.getType() == ListingType.TRADE_GIFT) {
			// Hediye/Takas ise, mobilden adam yanlışlıkla fiyat bile yollasa biz onu
			// sıfırlıyoruz. Veri temizliği!
			request.setPrice(null);
		}

		// Kontrolleri geçtik, ilanı paketliyoruz
		MarketplaceListing newListing = new MarketplaceListing();
		BeanUtils.copyProperties(request, newListing);
		newListing.setUser(dbUser);
		newListing.setNeighborhood(dbUser.getNeighborhood());
		newListing.setStatus(ListingStatus.ACTIVE); // İlan ilk açıldığında yayında olur

		MarketplaceListing savedListing = marketplaceRepository.save(newListing);

		// Geriye tertemiz bir DTO dönüyoruz
		DtoListing response = new DtoListing();
		BeanUtils.copyProperties(savedListing, response);
		response.setSellerId(dbUser.getId());
		response.setSellerFirstName(dbUser.getFirstname());
		response.setSellerLastName(dbUser.getLastname());

		return response;
	}

	// 2. MAHALLE DAYANIŞMASI (Home V.2 İlanları Listeleme)
	public Page<DtoListing> getNeighborhoodListings(Integer userId, int pageNo, int pageSize) {

		User dbUser = userRepository.findById(userId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Mahalleni seçmeden pazar yerini göremezsin!"));
		}

		Pageable pageable = PageRequest.of(pageNo, pageSize);

		// DİKKAT: Artık userId'yi de veriyoruz ki onu dışlasın!
		Page<MarketplaceListing> listingPage = marketplaceRepository
				.findByNeighborhoodIdAndStatusAndUserIdNotOrderByCreatedAtDesc(dbUser.getNeighborhood().getId(),
						ListingStatus.ACTIVE, userId, pageable);

		return listingPage.map(listing -> {
			DtoListing dto = new DtoListing();
			BeanUtils.copyProperties(listing, dto);
			dto.setSellerId(listing.getUser().getId());
			dto.setSellerFirstName(listing.getUser().getFirstname());
			dto.setSellerLastName(listing.getUser().getLastname());
			return dto;
		});
	}

	@Override
	// 3. KENDİ İLANLARIM SAYFASI (Sadece Benim Paylaştıklarım)
	public Page<DtoListing> getMyListings(Integer userId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);

		// Burada mahalleye veya statüye bakmıyoruz, adamın kendi profili sonuçta!
		Page<MarketplaceListing> myListingPage = marketplaceRepository.findByUserIdOrderByCreatedAtDesc(userId,
				pageable);

		return myListingPage.map(listing -> {
			DtoListing dto = new DtoListing();
			BeanUtils.copyProperties(listing, dto);
			dto.setSellerId(listing.getUser().getId());
			dto.setSellerFirstName(listing.getUser().getFirstname());
			dto.setSellerLastName(listing.getUser().getLastname());
			return dto;
		});
	}

	@Override
	// 4. İLAN DURUMUNU GÜNCELLE (Satıldı veya Silindi Yapma)
	public DtoListing updateListingStatus(Integer userId, Integer listingId, ListingStatus newStatus) {

		// 1. Önce ilanı veritabanından bul
		MarketplaceListing listing = marketplaceRepository.findById(listingId).orElseThrow(() -> new BaseException(
				new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kanzi böyle bir ilan bulunamadı!")));

		// 🚨 SENIOR GUARD CLAUSE (Güvenlik Duvarı) 🚨
		// Bu ilanı gerçekten bu isteği atan adam mı oluşturmuş? Başkasının ilanına
		// çökemesin!
		if (!listing.getUser().getId().equals(userId)) {
			throw new BaseException(
					new ErrorMessage(MessageType.GENERAL_EXCEPTION, "Başkasının ilanını değiştiremezsin kıral!"));
		}

		// 2. Güvenliği geçtiyse durumu güncelle ve kaydet
		listing.setStatus(newStatus);
		MarketplaceListing updatedListing = marketplaceRepository.save(listing);

		// 3. Ekranda hemen güncel halini göstermek için DTO'ya çevir ve dön
		DtoListing response = new DtoListing();
		BeanUtils.copyProperties(updatedListing, response);
		response.setSellerId(updatedListing.getUser().getId());
		response.setSellerFirstName(updatedListing.getUser().getFirstname());
		response.setSellerLastName(updatedListing.getUser().getLastname());

		return response;
	}
}