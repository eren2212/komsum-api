package com.ereniridere.service;

import java.util.List;
import java.util.Map;

public interface IFcmService {

	void sendToToken(String token, String title, String body, Map<String, String> data);

	void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data);
}
