-- ==============================================
-- 管理员账号创建脚本
-- ==============================================
-- 说明：此脚本用于创建系统管理员账号
-- 默认用户名：admin
-- 默认密码：admin123
-- BCrypt 哈希使用强度 10 生成
-- ==============================================

-- 创建管理员账号（PostgreSQL）
INSERT INTO app_users (
    user_id,
    username,
    password_hash,
    nickname,
    email,
    phone,
    role,
    status,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.PvEj7qSJ/SHsJ4Z8fK',  -- admin123 的 BCrypt 哈希
    '系统管理员',
    'admin@actrade.local',
    '13800138000',
    'ROLE_ADMIN',
    'NORMAL',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- 验证插入成功
SELECT user_id, username, nickname, role, status
FROM app_users
WHERE username = 'admin';

-- ==============================================
-- 如果需要重置管理员密码，执行以下语句
-- ==============================================
-- UPDATE app_users
-- SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.PvEj7qSJ/SHsJ4Z8fK',
--     updated_at = CURRENT_TIMESTAMP
-- WHERE username = 'admin';
