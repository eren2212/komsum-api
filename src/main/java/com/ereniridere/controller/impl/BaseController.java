package com.ereniridere.controller.impl;

import com.ereniridere.entity.RootEntity;

public class BaseController {

	public <T> RootEntity<T> ok(T data) {
		return RootEntity.ok(data);
	}

	public <T> RootEntity<T> Error(String errorMessage) {
		return RootEntity.error(errorMessage);
	}
}
