package com.csci4060.app.services;

import com.csci4060.app.model.authentication.ConfirmationToken;

public interface ConfirmationTokenService {

	ConfirmationToken findByConfirmationToken(String confirmationToken);
	ConfirmationToken save(ConfirmationToken token);
}
