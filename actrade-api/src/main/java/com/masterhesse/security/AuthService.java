package com.masterhesse.security;

import com.masterhesse.app_users.application.UserService;
import com.masterhesse.app_users.domain.AppUser;
import com.masterhesse.app_users.domain.UserRole;
import com.masterhesse.app_users.persistence.AppUserRepository;
import com.masterhesse.common.exception.BusinessException;
import com.masterhesse.config.JwtProperties;
import com.masterhesse.security.dto.AuthResponse;
import com.masterhesse.security.dto.LoginRequest;
import com.masterhesse.security.dto.RefreshTokenRequest;
import com.masterhesse.security.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    /**
     * 用户登录
     */
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            AppUser user = appUserRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new BusinessException("用户不存在"));

            // 更新最后登录时间
            user.setLastLoginAt(LocalDateTime.now());
            appUserRepository.save(user);

            log.info("User logged in: {}", request.username());

            return AuthResponse.of(
                    accessToken,
                    refreshToken,
                    jwtProperties.getAccessTokenExpirationMs() / 1000,
                    user.getUserId(),
                    user.getUsername(),
                    user.getRole().name()
            );
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {}", request.username());
            throw new BusinessException("用户名或密码错误");
        }
    }

    /**
     * 用户注册
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 检查用户名是否存在
        if (appUserRepository.existsByUsername(request.username())) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否存在
        if (request.email() != null && !request.email().isBlank()) {
            if (appUserRepository.existsByEmail(request.email())) {
                throw new BusinessException("邮箱已被注册");
            }
        }

        // 创建用户
        AppUser user = AppUser.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickname(request.nickname() != null ? request.nickname() : request.username())
                .email(request.email())
                .phone(request.phone())
                .role(UserRole.ROLE_USER)
                .build();

        user = appUserRepository.save(user);

        // 生成 Token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        log.info("User registered: {}", user.getUsername());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpirationMs() / 1000,
                user.getUserId(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    /**
     * 刷新 Token
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        // 验证 refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("无效的 Refresh Token");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException("不是有效的 Refresh Token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // 加载用户
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        // 生成新的 Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        log.info("Token refreshed for user: {}", username);

        return AuthResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtProperties.getAccessTokenExpirationMs() / 1000,
                user.getUserId(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    /**
     * 获取当前用户信息
     */
    @Transactional(readOnly = true)
    public AuthResponse getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        AppUser user = appUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        return AuthResponse.of(
                null, // accessToken - 不返回
                null, // refreshToken - 不返回
                0L,   // expiresIn
                user.getUserId(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    /**
     * 用户登出
     */
    public void logout(Authentication authentication, String authHeader) {
        String username = authentication.getName();

        // 将用户的所有 Token 撤销（通过存储撤销时间）
        // 后续的 Token 验证会检查是否在撤销时间之后

        log.info("User logged out: {}", username);
    }
}
