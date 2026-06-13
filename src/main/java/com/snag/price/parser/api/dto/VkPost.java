package com.snag.price.parser.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VkPost implements ParsedPost {
    private Long   id;
    private String text;
    private Long   date;
    private String imageUrl;
    private String sourceUrl;
    private String promoCode;
    private String discountAmount;
    private String productName;
    private int    likes;
    private int    reposts;

    private String sourceName;

    @Override
    public String getExternalId() {
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public long getDate() {
        return date != null ? date : 0L;
    }
}