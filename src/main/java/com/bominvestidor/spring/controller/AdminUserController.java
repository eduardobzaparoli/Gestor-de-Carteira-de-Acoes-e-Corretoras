package com.bominvestidor.spring.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bominvestidor.spring.dto.admin.AdminUserCreateRequest;
import com.bominvestidor.spring.dto.admin.AdminUserResponse;
import com.bominvestidor.spring.dto.admin.AdminUserUpdateRequest;
import com.bominvestidor.spring.service.admin.AdminUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

	private final AdminUserService adminUserService;

	public AdminUserController(AdminUserService adminUserService) {
		this.adminUserService = adminUserService;
	}

	@GetMapping
	public List<AdminUserResponse> findAll() {
		return adminUserService.findAll();
	}

	@GetMapping("/{userId}")
	public AdminUserResponse findById(@PathVariable UUID userId) {
		return adminUserService.findById(userId);
	}

	@PostMapping
	public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody AdminUserCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.create(request));
	}

	@PutMapping("/{userId}")
	public AdminUserResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
			@Valid @RequestBody AdminUserUpdateRequest request) {
		return adminUserService.update(UUID.fromString(jwt.getSubject()), userId, request);
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
		adminUserService.deactivate(UUID.fromString(jwt.getSubject()), userId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{userId}/reactivate")
	public AdminUserResponse reactivate(@PathVariable UUID userId) {
		return adminUserService.reactivate(userId);
	}
}
