package com.bominvestidor.spring.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bominvestidor.spring.dto.auth.AuthenticationResponse;
import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.dto.auth.UpdateProfileRequest;
import com.bominvestidor.spring.dto.user.PublicUserResponse;
import com.bominvestidor.spring.service.auth.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<PublicUserResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	public AuthenticationResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/me")
	public PublicUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
		return authService.currentUser(jwt.getSubject());
	}

	@PutMapping("/me")
	@PreAuthorize("hasRole('INVESTOR')")
	public PublicUserResponse updateCurrentUser(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UpdateProfileRequest request) {
		return authService.updateCurrentUser(jwt.getSubject(), request);
	}
}
