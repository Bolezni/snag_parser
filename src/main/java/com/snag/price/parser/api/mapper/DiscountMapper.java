package com.snag.price.parser.api.mapper;

import com.snag.price.parser.api.dto.ParsedPost;
import com.snag.price.parser.api.dto.VkPost;
import com.snag.price.parser.model.Discount;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;

@Component
public class DiscountMapper {

    public Discount toDiscount(ParsedPost post) {
        return Discount.builder()
                .externalId(post.getExternalId())
                .source(post.getSourceName())
                .title(truncate(post.getProductName(), 500))
                .description(post.getText())
                .promoCode(post.getPromoCode())
                .discountPercent(parsePercent(post.getDiscountAmount()))
                .sourceURL(post.getSourceUrl())
                .imageURL(post.getImageUrl())
                .promoStart(toLocalDateTime(post.getDate()))
                .contentHash(computeHash(post))
                .active(true)
                .build();
    }

    public VkPost toVkPost(Discount discount){
        return VkPost.builder()
                .id(Long.valueOf(discount.getExternalId()))
                .text(discount.getTitle())
                .date(discount.getPromoStart()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli())
                .imageUrl(discount.getImageURL())
                .sourceUrl(discount.getSourceURL())
                .discountAmount(String.valueOf(discount.getDiscountPercent()))
                .promoCode(discount.getPromoCode())
                .build();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }


    private Integer parsePercent(String discountAmount) {
        if (discountAmount == null) return null;
        try {
            String digits = discountAmount.replaceAll("[^\\d]", "");
            return digits.isBlank() ? null : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String computeHash(ParsedPost post) {
        String raw = post.getSourceName() + "|" + post.getExternalId();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }

    private LocalDateTime toLocalDateTime(long unix) {
        if (unix == 0) return LocalDateTime.now();
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(unix), ZoneId.systemDefault());
    }
}