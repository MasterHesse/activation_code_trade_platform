package com.masterhesse.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类 - 用于生成 BCrypt 加密密码
 * 运行此类的 main 方法可生成密码哈希
 */
public class PasswordGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 生成管理员密码哈希
        String adminPassword = "admin123";
        String hash = encoder.encode(adminPassword);

        System.out.println("=== 管理员账号创建指南 ===");
        System.out.println();
        System.out.println("密码: " + adminPassword);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println();
        System.out.println("=== SQL 插入语句 ===");
        System.out.println();
        System.out.println("INSERT INTO app_users (");
        System.out.println("    user_id, username, password_hash, nickname, email, phone, ");
        System.out.println("    role, status, created_at, updated_at");
        System.out.println(") VALUES (");
        System.out.println("    gen_random_uuid(),");
        System.out.println("    'admin',");
        System.out.println("    '" + hash + "',");
        System.out.println("    '系统管理员',");
        System.out.println("    'admin@actrade.local',");
        System.out.println("    '13800138000',");
        System.out.println("    'ROLE_ADMIN',");
        System.out.println("    'NORMAL',");
        System.out.println("    CURRENT_TIMESTAMP,");
        System.out.println("    CURRENT_TIMESTAMP");
        System.out.println(");");
    }
}
