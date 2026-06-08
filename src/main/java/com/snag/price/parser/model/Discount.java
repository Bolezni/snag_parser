package com.snag.price.parser.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "discounts")
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column
    private String source;

    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "promo_code")
    private String promoCode;

    @Column(name = "price_regular", precision = 10, scale = 2)
    private BigDecimal priceRegular;

    @Column(name = "price_discount", precision = 10, scale = 2)
    private BigDecimal priceDiscount;

    @Column(name = "discount_percent")
    private Integer discountPercent;

    @Column(name = "image_url", length = 1000)
    private String imageURL;

    @Column(name = "source_url", length = 1000)
    private String sourceURL;

    @Column(name = "content_hash", unique = true)
    private String contentHash;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime promoStart;

    private LocalDateTime promoEnd;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
