package com.bominvestidor.spring.mapper.user;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.domain.user.User;
import com.bominvestidor.spring.dto.user.PublicUserResponse;
import com.bominvestidor.spring.entity.user.UserEntity;

@Component
public class UserMapper {

	public User toDomain(UserEntity entity) {
		return new User(
				entity.getId(),
				entity.getName(),
				entity.getEmail(),
				entity.getPasswordHash(),
				entity.getRole(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}

	public UserEntity toEntity(User user) {
		return new UserEntity(
				user.id(),
				user.name(),
				user.email(),
				user.passwordHash(),
				user.role(),
				user.status(),
				user.createdAt(),
				user.updatedAt());
	}

	public PublicUserResponse toPublicResponse(User user) {
		return new PublicUserResponse(user.id(), user.name(), user.email(), user.role());
	}
}
