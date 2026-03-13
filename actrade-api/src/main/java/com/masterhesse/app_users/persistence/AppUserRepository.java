package com.masterhesse.app_users.persistence;

import com.masterhesse.app_users.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID>, JpaSpecificationExecutor<AppUser> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndUserIdNot(String username, UUID userId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndUserIdNot(String email, UUID userId);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndUserIdNot(String phone, UUID userId);
}