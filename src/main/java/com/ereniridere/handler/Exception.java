package com.ereniridere.handler;

import java.util.Date;

import lombok.Data;

@Data
public class Exception<E> {

	// NOT: Buradaki hostName alanı kaldırıldı. Sunucunun makine adı her hata
	// yanıtıyla birlikte istemciye gidiyordu; dışarıya sızdırılmaması gereken
	// altyapı bilgisidir ve istemcinin hiçbir işine yaramıyordu.

	private String path;

	private Date createTime;

	private E message;
}
