package com.bominvestidor.spring.repository.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.domain.user.UserStatus;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

	boolean existsByEmail(String email);

	Optional<UserEntity> findByEmail(String email);

	List<UserEntity> findAllByOrderByCreatedAtAscIdAsc();

	long countByRoleAndStatus(UserRole role, UserStatus status);
}
