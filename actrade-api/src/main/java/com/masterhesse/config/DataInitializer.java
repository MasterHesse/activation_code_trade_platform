package com.masterhesse.config;

import com.masterhesse.app_users.domain.AppUser;
import com.masterhesse.app_users.domain.UserRole;
import com.masterhesse.app_users.domain.UserStatus;
import com.masterhesse.app_users.persistence.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 数据初始化配置
 * 应用启动时自动初始化默认数据
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 初始化默认管理员账号
     * 仅当管理员不存在时才创建
     */
    @Bean
    public CommandLineRunner initAdminUser() {
        return args -> {
            initAdmin();
            initDefaultCategory();
        };
    }

    private void initAdmin() {
        String adminUsername = "admin";

        if (appUserRepository.existsByUsername(adminUsername)) {
            log.info("管理员账号已存在，跳过创建");
            return;
        }

        AppUser admin = AppUser.builder()
                .username(adminUsername)
                .passwordHash(passwordEncoder.encode("admin123"))
                .nickname("系统管理员")
                .email("admin@actrade.local")
                .phone("13800138000")
                .role(UserRole.ROLE_ADMIN)
                .status(UserStatus.NORMAL)
                .build();

        appUserRepository.save(admin);
        log.info("===========================================");
        log.info("  默认管理员账号创建成功！");
        log.info("  用户名: {}", adminUsername);
        log.info("  密码: admin123");
        log.info("  首次登录后请立即修改密码！");
        log.info("===========================================");
    }

    private void initDefaultCategory() {
        // TODO: 初始化默认商品分类
        log.info("商品分类初始化（待实现）");
    }
}
