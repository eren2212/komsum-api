package com.ereniridere.dto.response.User;

import lombok.Data;

@Data
public class DtoNeighborhood {

	private Integer id; // Mahalle ID – register isteğinde neighborhoodId olarak kullanılır

	private String city; // Örn: İstanbul

	private String district; // Örn: Beşiktaş

	private String name; // Örn: Sinanpaşa Mahallesi

}
