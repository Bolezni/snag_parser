package com.snag.price.parser.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VkPost {
    private Long id;
    private String text;
    private Long date;        // unix timestamp
    private String imageUrl;
    private Integer likes;
    private Integer reposts;
    private String sourceUrl; // ссылка на пост

    private String productName;
    private String discountAmount;  // "-30%" или "скидка 500 руб"
    private String promoCode;
}