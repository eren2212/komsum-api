package com.ereniridere.dto.response.post;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cursor (keyset) tabanlı feed dilimi.
 *
 * Offset/sayfa numarası yerine kullanılır: sürekli yeni post girse bile
 * duplicate/atlama olmaz çünkü sabit bir sıralama noktasından (nextCursor)
 * geriye doğru ilerlenir.
 *
 * nextCursor: bir sonraki (daha eski) sayfayı istemek için client'ın aynen geri
 * yollayacağı opak token. null ise başka sayfa yoktur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoPostSlice {

	private List<DtoPost> content;

	// Client bunu inceleme yapmadan bir sonraki isteğe aynen geçirir (opak).
	// Standart akışta "createdAt|id", sponsorlu yakınlık akışında sayfa numarasıdır.
	private String nextCursor;

	private boolean hasNext;
}
