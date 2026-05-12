package com.ereniridere.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ereniridere.dto.external.ExternalDistrict;
import com.ereniridere.dto.external.ExternalNeighborhood;
import com.ereniridere.dto.external.ExternalProvince;

import lombok.extern.slf4j.Slf4j;

/**
 * https://api.turkiyeapi.dev/v1 entegrasyonu.
 *
 * Gerçek API response formatı:
 *
 * GET /provinces           → data: [ {id, name, districts:[{id,name,...}]} ]
 * GET /provinces/{id}      → data: { id, name, districts:[{id,name,...}] }
 *
 * GET /districts?provinceId={id}
 *   → data: [ {id, name, provinceId, province:"İstanbul"(STRING!), ...} ]
 *
 * GET /neighborhoods?districtId={id}
 *   → data: [ {id, name, districtId, district:"Beşiktaş"(STRING!),
 *              provinceId, province:"İstanbul"(STRING!)} ]
 *
 * GET /neighborhoods/{id}
 *   → data: { id, name, districtId, district, provinceId, province }
 *
 * ÖNEMLİ: Jackson, JSON sayılarını Long veya Integer olarak deserialize edebilir.
 * Tüm ID dönüşümleri Number.intValue() ile yapılır.
 */
@Slf4j
@Component
public class TurkiyeApiClient {

	private static final String BASE_URL = "https://api.turkiyeapi.dev/v1";

	private final RestClient restClient;

	public TurkiyeApiClient() {
		this.restClient = RestClient.builder()
				.baseUrl(BASE_URL)
				.defaultHeader("Accept", "application/json")
				.build();
	}

	// ─── İller ───────────────────────────────────────────────────────────────
	// GET /provinces
	// data: [ {id, name, districts:[...], ...} ]

	public List<ExternalProvince> fetchCities() {
		try {
			var response = get("/provinces", new ParameterizedTypeReference<Map<String, Object>>() {});
			if (response == null) return Collections.emptyList();

			@SuppressWarnings("unchecked")
			var data = (List<Map<String, Object>>) response.get("data");
			if (data == null) return Collections.emptyList();

			return data.stream()
					.map(p -> {
						ExternalProvince province = new ExternalProvince();
						province.setId(toInt(p.get("id")));
						province.setName((String) p.get("name"));
						return province;
					})
					.toList();

		} catch (Exception e) {
			log.error("TurkiyeApi il listesi alınamadı: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

	// ─── İlçeler ─────────────────────────────────────────────────────────────
	// GET /districts?provinceId={cityId}
	// data: [ {id, name, provinceId:34, province:"İstanbul"(STRING), ...} ]

	public List<ExternalDistrict> fetchDistrictsByCityId(Integer cityId) {
		try {
			var response = restClient.get()
					.uri("/districts?provinceId={provinceId}", cityId)
					.retrieve()
					.body(new ParameterizedTypeReference<Map<String, Object>>() {});

			if (response == null) return Collections.emptyList();

			@SuppressWarnings("unchecked")
			var data = (List<Map<String, Object>>) response.get("data");
			if (data == null) return Collections.emptyList();

			return data.stream()
					.map(d -> {
						// province alanı STRING olarak geliyor (object değil)
						ExternalProvince province = new ExternalProvince();
						province.setId(toInt(d.get("provinceId")));
						province.setName((String) d.get("province"));

						ExternalDistrict district = new ExternalDistrict();
						district.setId(toInt(d.get("id")));
						district.setName((String) d.get("name"));
						district.setProvince(province);
						return district;
					})
					.toList();

		} catch (Exception e) {
			log.error("TurkiyeApi ilçe listesi alınamadı (cityId={}): {}", cityId, e.getMessage());
			return Collections.emptyList();
		}
	}

	// ─── Mahalleler ───────────────────────────────────────────────────────────
	// GET /neighborhoods?districtId={districtId}
	// data: [ {id, name, districtId:1183, district:"Beşiktaş"(STRING),
	//          provinceId:34, province:"İstanbul"(STRING)} ]

	public List<ExternalNeighborhood> fetchNeighborhoodsByDistrictId(Integer districtId) {
		try {
			var response = restClient.get()
					.uri("/neighborhoods?districtId={districtId}", districtId)
					.retrieve()
					.body(new ParameterizedTypeReference<Map<String, Object>>() {});

			if (response == null) return Collections.emptyList();

			@SuppressWarnings("unchecked")
			var data = (List<Map<String, Object>>) response.get("data");
			if (data == null) return Collections.emptyList();

			return data.stream()
					.map(n -> {
						// district ve province STRING olarak geliyor
						ExternalProvince province = new ExternalProvince();
						province.setId(toInt(n.get("provinceId")));
						province.setName((String) n.get("province"));

						ExternalDistrict district = new ExternalDistrict();
						district.setId(toInt(n.get("districtId")));
						district.setName((String) n.get("district"));
						district.setProvince(province);

						ExternalNeighborhood neighborhood = new ExternalNeighborhood();
						neighborhood.setId(toInt(n.get("id")));
						neighborhood.setName((String) n.get("name"));
						neighborhood.setDistrict(district);
						return neighborhood;
					})
					.toList();

		} catch (Exception e) {
			log.error("TurkiyeApi mahalle listesi alınamadı (districtId={}): {}", districtId, e.getMessage());
			return Collections.emptyList();
		}
	}

	// ─── Tek mahalle (kayıt akışında upsert için) ─────────────────────────────
	// GET /neighborhoods/{id}
	// data: { id, name, districtId, district(STRING), provinceId, province(STRING) }

	public Optional<ExternalNeighborhood> fetchNeighborhoodById(Integer id) {
		try {
			var response = get("/neighborhoods/{id}", new ParameterizedTypeReference<Map<String, Object>>() {}, id);
			if (response == null) return Optional.empty();

			@SuppressWarnings("unchecked")
			var data = (Map<String, Object>) response.get("data");
			if (data == null) return Optional.empty();

			ExternalProvince province = new ExternalProvince();
			province.setId(toInt(data.get("provinceId")));
			province.setName((String) data.get("province"));

			ExternalDistrict district = new ExternalDistrict();
			district.setId(toInt(data.get("districtId")));
			district.setName((String) data.get("district"));
			district.setProvince(province);

			ExternalNeighborhood neighborhood = new ExternalNeighborhood();
			neighborhood.setId(toInt(data.get("id")));
			neighborhood.setName((String) data.get("name"));
			neighborhood.setDistrict(district);

			return Optional.of(neighborhood);

		} catch (Exception e) {
			log.error("TurkiyeApi mahalle bulunamadı (id={}): {}", id, e.getMessage());
			return Optional.empty();
		}
	}

	// ─── Yardımcılar ─────────────────────────────────────────────────────────

	private <T> T get(String uri, ParameterizedTypeReference<T> type, Object... uriVars) {
		return restClient.get()
				.uri(uri, uriVars)
				.retrieve()
				.body(type);
	}

	/** Jackson bazen Integer bazen Long döner; her ikisini de güvenle handle eder. */
	private static Integer toInt(Object value) {
		if (value == null) return null;
		return ((Number) value).intValue();
	}
}
