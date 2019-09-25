package com.csci4060.app.services;

import java.util.List;

import com.csci4060.app.model.User;

public interface UserService {

	User save(User user);
	
	List<User> findAll();
	
	void delete(String email);
	
	User findByEmail(String email);
	
	User findByUsername(String username);
	
	User findById(Long id);
	
	User update(User user);
	
	Boolean existsByUsername(String username);
	
	Boolean existsByEmail(String email);
}
