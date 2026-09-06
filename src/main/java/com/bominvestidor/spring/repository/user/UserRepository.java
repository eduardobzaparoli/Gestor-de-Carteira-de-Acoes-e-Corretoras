package com.bominvestidor.spring.repository.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.domain.user.UserStatus;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

	boolean existsByEmail(String email);

	Optional<UserEntity> findByEmail(String email);

	List<UserEntity> findAllByOrderByCreatedAtAscIdAsc();

	long countByRoleAndStatus(UserRole role, UserStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from UserEntity u where u.role = :role and u.status = :status order by u.id")
	List<UserEntity> findAllByRoleAndStatusForUpdate(@Param("role") UserRole role,
			@Param("status") UserStatus status);
}
