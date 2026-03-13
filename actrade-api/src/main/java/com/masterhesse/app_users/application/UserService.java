package com.masterhesse.app_users.application;

import com.masterhesse.app_users.api.request.CreateUserRequest;
import com.masterhesse.app_users.api.request.UpdateUserRequest;
import com.masterhesse.app_users.api.response.UserResponse;
import com.masterhesse.app_users.domain.AppUser;
import com.masterhesse.app_users.domain.UserRole;
import com.masterhesse.app_users.domain.UserStatus;
import com.masterhesse.app_users.persistence.AppUserRepository;
import com.masterhesse.app_users.persistence.AppUserSpecifications;
import com.masterhesse.common.exception.BusinessException;
import com.masterhesse.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = request.username().trim();
        String email = normalizeNullable(request.email());
        String phone = normalizeNullable(request.phone());

        validateUniqueForCreate(username, email, phone);

        AppUser user = AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickname(normalizeNullable(request.nickname()))
                .email(email)
                .phone(phone)
                .role(request.role() != null ? request.role() : UserRole.ROLE_USER)
                .status(request.status() != null ? request.status() : UserStatus.NORMAL)
                .build();

        return UserResponse.from(appUserRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID userId) {
        return UserResponse.from(getActiveUser(userId));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> page(String keyword, UserRole role, UserStatus status, int page, int size) {
        Specification<AppUser> spec = buildSpec(keyword, role, status);

        return appUserRepository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).map(UserResponse::from);
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateUserRequest request) {
        AppUser user = getActiveUser(userId);

        if (request.username() != null) {
            String username = request.username().trim();
            if (!username.equals(user.getUsername())
                    && appUserRepository.existsByUsernameAndUserIdNot(username, userId)) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(username);
        }

        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (request.nickname() != null) {
            user.setNickname(normalizeNullable(request.nickname()));
        }

        if (request.email() != null) {
            String email = normalizeNullable(request.email());
            if (email != null
                    && !email.equals(user.getEmail())
                    && appUserRepository.existsByEmailAndUserIdNot(email, userId)) {
                throw new BusinessException("邮箱已存在");
            }
            user.setEmail(email);
        }

        if (request.phone() != null) {
            String phone = normalizeNullable(request.phone());
            if (phone != null
                    && !phone.equals(user.getPhone())
                    && appUserRepository.existsByPhoneAndUserIdNot(phone, userId)) {
                throw new BusinessException("手机号已存在");
            }
            user.setPhone(phone);
        }

        if (request.role() != null) {
            user.setRole(request.role());
        }

        if (request.status() != null) {
            user.setStatus(request.status());
        }

        return UserResponse.from(appUserRepository.save(user));
    }

    @Transactional
    public void delete(UUID userId) {
        AppUser user = getActiveUser(userId);
        user.setStatus(UserStatus.DELETED);
        appUserRepository.save(user);
    }

    private AppUser getActiveUser(UUID userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new ResourceNotFoundException("用户不存在");
        }

        return user;
    }

    private void validateUniqueForCreate(String username, String email, String phone) {
        if (appUserRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }
        if (email != null && appUserRepository.existsByEmail(email)) {
            throw new BusinessException("邮箱已存在");
        }
        if (phone != null && appUserRepository.existsByPhone(phone)) {
            throw new BusinessException("手机号已存在");
        }
    }

    private Specification<AppUser> buildSpec(String keyword, UserRole role, UserStatus status) {
        Specification<AppUser> spec = Specification.where(AppUserSpecifications.notDeleted());

        if (StringUtils.hasText(keyword)) {
            spec = spec.and(AppUserSpecifications.keywordLike(keyword));
        }
        if (role != null) {
            spec = spec.and(AppUserSpecifications.hasRole(role));
        }
        if (status != null) {
            spec = spec.and(AppUserSpecifications.hasStatus(status));
        }

        return spec;
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}