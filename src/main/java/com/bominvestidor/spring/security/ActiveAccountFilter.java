package com.bominvestidor.spring.security;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bominvestidor.spring.domain.user.UserStatus;
import com.bominvestidor.spring.repository.user.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ActiveAccountFilter extends OncePerRequestFilter {

	private final UserRepository userRepository;
	private final SecurityErrorWriter errorWriter;

	public ActiveAccountFilter(UserRepository userRepository, SecurityErrorWriter errorWriter) {
		this.userRepository = userRepository;
		this.errorWriter = errorWriter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authorization = request.getHeader("Authorization");
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			try {
				UUID userId = UUID.fromString(jwt.getSubject());
				var user = userRepository.findById(userId).orElse(null);
				if (user == null) {
					reject(response, "UNAUTHORIZED", "Authentication is required or the token is invalid", request);
					return;
				}
				if (user.getStatus() != UserStatus.ACTIVE) {
					reject(response, "ACCOUNT_INACTIVE", "Account is inactive", request);
					return;
				}
			}
			catch (IllegalArgumentException exception) {
				reject(response, "UNAUTHORIZED", "Authentication is required or the token is invalid", request);
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

	private void reject(HttpServletResponse response, String code, String message, HttpServletRequest request)
			throws IOException {
		SecurityContextHolder.clearContext();
		errorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, code, message, request.getRequestURI());
	}
}
