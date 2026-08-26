package com.bominvestidor.spring.repository.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bominvestidor.spring.entity.user.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

	boolean existsByEmail(String email);

	Optional<UserEntity> findByEmail(String email);
}
