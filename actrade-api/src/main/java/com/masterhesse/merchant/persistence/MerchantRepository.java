package com.masterhesse.merchant.persistence;

import com.masterhesse.merchant.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}