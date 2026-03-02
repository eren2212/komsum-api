package com.ereniridere.entity;

import lombok.Data;

@Data
public class RootEntity<T> {

	private boolean result;

	private String errormessage;

	private T data;

	public static <T> RootEntity<T> ok(T data) {
		RootEntity<T> rootEntity = new RootEntity<>();

		rootEntity.setResult(true);
		rootEntity.setErrormessage(null);
		rootEntity.setData(data);

		return rootEntity;
	}

	public static <T> RootEntity<T> error(String error) {
		RootEntity<T> rootEntity = new RootEntity<>();

		rootEntity.setResult(false);
		rootEntity.setData(null);
		rootEntity.setErrormessage(error);

		return rootEntity;

	}
}
