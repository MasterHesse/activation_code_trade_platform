package com.masterhesse.security;

import com.masterhesse.app_users.domain.AppUser;
import com.masterhesse.app_users.domain.UserStatus;
import com.masterhesse.app_users.persistence.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        if (appUser.getStatus() == UserStatus.DELETED) {
            throw new UsernameNotFoundException("用户已被删除: " + username);
        }

        if (appUser.getStatus() == UserStatus.DISABLED) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        return new User(
                appUser.getUsername(),
                appUser.getPasswordHash(),
                appUser.getStatus() == UserStatus.NORMAL,
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority(appUser.getRole().name()))
        );
    }
}
