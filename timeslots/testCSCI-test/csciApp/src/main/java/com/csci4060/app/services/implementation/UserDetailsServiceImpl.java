package com.csci4060.app.services.implementation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csci4060.app.model.User;
import com.csci4060.app.model.authentication.UserPrinciple;
import com.csci4060.app.repository.UserRepository;
import com.csci4060.app.services.UserService;

/*
 *  UserDetailsServiceImpl implements UserDetailsService and overrides loadUserByUsername() method.
 *  loadUserByUsername method finds a record from users database tables to build a UserDetails object
 *  for authentication.
 */
@Service (value = "userService")
public class UserDetailsServiceImpl implements UserDetailsService, UserService {

	@Autowired
	UserRepository userRepo;
	
	@Autowired
	PasswordEncoder encoder;

	@Override
	@Transactional
	// UserPrinciple implements UserDetails so returning UserPrinciple doesn't cause
	// any problems
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepo.findByUsername(username).orElseThrow(
				() -> new UsernameNotFoundException("User not found with -> username or email: " + username));

		return UserPrinciple.build(user);
	}

	@Override
	public List<User> findAll() {
		return userRepo.findAll();
	}
	
	@Override
	public User findByUsername(String username) {
		return userRepo.findByUsername(username)
				.orElseThrow(()->new UsernameNotFoundException("User not found with -> username: "+username));
	}

	@Override
	public User findByEmail(String email) {
		return userRepo.findByEmailIgnoreCase(email)
				.orElseThrow(()->new UsernameNotFoundException("User not found with -> email:"+email));
	}

	@Override
	public User findById(Long id) {
		return userRepo.findById(id)
				.orElseThrow(()->new UsernameNotFoundException("User not found"));
	}

	@Override
	public User update(User user) {
		User userFromDB = userRepo.findById(user.getId())
				.orElseThrow(()-> new UsernameNotFoundException("User not found"));
		userFromDB.setName(user.getName());
		userFromDB.setEmail(user.getEmail());
		userFromDB.setUsername(user.getUsername());
		userFromDB.setPassword(encoder.encode(user.getPassword()));
		return userFromDB;
	}

	@Override
	public User save(User user) {
		return userRepo.save(user);
	}
	
	@Override
	public void delete(String email) {
		
		User user = userRepo.findByEmailIgnoreCase(email)
				.orElseThrow(()->new UsernameNotFoundException("User not found with -> email:"+email));
		userRepo.delete(user);
	}

	@Override
	public Boolean existsByUsername(String username) {
		return userRepo.existsByUsername(username);
	}

	@Override
	public Boolean existsByEmail(String email) {
		return userRepo.existsByEmail(email);
	}

}
