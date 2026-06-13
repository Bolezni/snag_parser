package com.snag.price.parser.api.dto;

import lombok.Data;

@Data
public class HlruPost implements ParsedPost{

    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private String sourceUrl;
    private String promoCode;
    private String discountAmount; // "-20%" или "Бесплатно"
    private String shopName;       // "Магнит", "Pix"
    private String shopLogoUrl;
    private String category;       // "Промокоды", "Скидки"
    private int    rating;         // температура (12°, 76°)
    private long   date;

    @Override public String getExternalId()    { return id; }
    @Override public String getProductName()   { return title; }
    @Override public String getText()          { return description; }
    @Override public String getImageUrl()      { return imageUrl; }
    @Override public String getSourceUrl()     { return sourceUrl; }
    @Override public String getPromoCode()     { return promoCode; }
    @Override public String getDiscountAmount(){ return discountAmount; }
    @Override public String getSourceName()    { return "HLRU"; }
    @Override public long   getDate()          { return date; }
}
