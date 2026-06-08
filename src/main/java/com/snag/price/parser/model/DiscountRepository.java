package com.snag.price.parser.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, UUID>, JpaSpecificationExecutor<Discount> {
    boolean existsByContentHash(String contentHash);

    boolean existsByExternalId(String externalId);

    Page<Discount> findBy(Pageable pageable);
}