package com.masterhesse.app_users.persistence;

import com.masterhesse.app_users.domain.AppUser;
import com.masterhesse.app_users.domain.UserRole;
import com.masterhesse.app_users.domain.UserStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class AppUserSpecifications {

    private AppUserSpecifications() {
    }

    public static Specification<AppUser> notDeleted() {
        return (root, query, cb) -> cb.notEqual(root.get("status"), UserStatus.DELETED);
    }

    public static Specification<AppUser> keywordLike(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        String like = "%" + keyword.trim().toLowerCase() + "%";

        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("username")), like),
                cb.like(cb.lower(root.get("nickname")), like),
                cb.like(cb.lower(root.get("email")), like),
                cb.like(cb.lower(root.get("phone")), like)
        );
    }

    public static Specification<AppUser> hasRole(UserRole role) {
        if (role == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    public static Specification<AppUser> hasStatus(UserStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}