package com.csci4060.app.controller;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.csci4060.app.configuration.jwt.JwtProvider;
import com.csci4060.app.model.APIresponse;
import com.csci4060.app.model.Role;
import com.csci4060.app.model.RoleName;
import com.csci4060.app.model.User;
import com.csci4060.app.model.authentication.ConfirmationToken;
import com.csci4060.app.model.authentication.JwtResponse;
import com.csci4060.app.model.authentication.LoginForm;
import com.csci4060.app.model.authentication.SignUpForm;
import com.csci4060.app.services.ConfirmationTokenService;
import com.csci4060.app.services.EmailSenderService;
import com.csci4060.app.services.RoleService;
import com.csci4060.app.services.UserService;

/*
 *  AuthRestAPIs.java defines 2 APIs:

/api/auth/signup: sign up
-> check username/email is already in use.
-> create User object
-> store to database
/api/auth/signin: sign in
-> attempt to authenticate with AuthenticationManager bean.
-> add authentication object to SecurityContextHolder
-> Generate JWT token, then return JWT to client

 */

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthRestAPIs {

	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserService userService;

	@Autowired 
	RoleService roleService;
	
	@Autowired
	ConfirmationTokenService confirmationTokenService;

	@Autowired
	private EmailSenderService emailSenderService;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	JwtProvider jwtProvider;

	@PostMapping("/signin")
	public APIresponse authenticateUser(@Valid @RequestBody LoginForm loginRequest) {
		
		User user = userService.findByUsername(loginRequest.getUsername());
		
//		if(user.isVerified()) {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			String jwt = jwtProvider.generateJwtToken(authentication);
			return new APIresponse(HttpStatus.OK.value(), "Successful", new JwtResponse(jwt, loginRequest.getUsername()));
		//}
		
		//return new APIresponse(HttpStatus.FORBIDDEN.value(), "Please click on the verification link to login", null);
	}

	@PostMapping("/signup")
	public APIresponse registerUser(@Valid @RequestBody SignUpForm signUpRequest) {
		if (userService.existsByUsername(signUpRequest.getUsername())) {
			return new APIresponse(HttpStatus.BAD_REQUEST.value(), "Fail -> Username is already taken!", null);
		}

		if (userService.existsByEmail(signUpRequest.getEmail())) {
			return new APIresponse(HttpStatus.BAD_REQUEST.value(), "Fail -> Email is already in use!", null);
		}

		// Creating user's account
		User user = new User(signUpRequest.getName(), signUpRequest.getUsername(), signUpRequest.getEmail(),
				encoder.encode(signUpRequest.getPassword()), signUpRequest.isVerified());

		Set<String> strRoles = signUpRequest.getRole();
		Set<Role> roles = new HashSet<>();

		for (String each : strRoles) {

			if (each.equals("admin")) {
				Role adminRole = roleService.findByName(RoleName.ROLE_ADMIN);
				roles.add(adminRole);
			} else if (each.equals("pm")) {
				Role pmRole = roleService.findByName(RoleName.ROLE_PM);
				roles.add(pmRole);
			} else {
				Role userRole = roleService.findByName(RoleName.ROLE_USER);
				roles.add(userRole);
			}
		}

		user.setRoles(roles);
		userService.save(user);
		
		ConfirmationToken confirmationToken = new ConfirmationToken(user);
		
		confirmationTokenService.save(confirmationToken);

		SimpleMailMessage mailMessage = new SimpleMailMessage();
		mailMessage.setTo(user.getEmail());
		mailMessage.setSubject("Complete Registration!");
		mailMessage.setFrom("ulmautoemail@gmail.com");
		mailMessage.setText("To confirm your account, please click here : "
				+ "http://localhost:8181/api/auth/confirm-account/" + confirmationToken.getConfirmationToken());

		emailSenderService.sendEmail(mailMessage);

		return new APIresponse(HttpStatus.OK.value(), "Verification code has been sent to you email address. Please click it to register successfully.", null);
	}

	@GetMapping("/confirm-account/{token}")
	public APIresponse confirmUserAccout(@PathVariable("token") String confirmationToken) {
		ConfirmationToken token = confirmationTokenService.findByConfirmationToken(confirmationToken);
		
		if(token != null)
        {
            User user = userService.findByEmail(token.getUser().getEmail());
            
            user.setVerified(true);
            
            userService.save(user);
            return new APIresponse(HttpStatus.OK.value(),"User registered successfully!", null);
        }
        else
        {
        	return new APIresponse(HttpStatus.UNAUTHORIZED.value(),"Verficication token is not in the database", null);
        }
	}

}
