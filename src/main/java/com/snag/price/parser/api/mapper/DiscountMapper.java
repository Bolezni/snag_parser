package com.snag.price.parser.mapper;

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

    public Discount toDiscount(VkPost post) {
        return Discount.builder()
                .externalId(String.valueOf(post.getId()))

                .source("VK_PROBNICK")

                .title(extractTitle(post.getText()))

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


    private String extractTitle(String text) {
        if (text == null || text.isBlank()) return "Без названия";
        return text.lines()
                .map(String::trim)
                .filter(l -> !l.isBlank())
                .findFirst()
                .map(l -> l.substring(0, Math.min(l.length(), 255)))
                .orElse("Без названия");
    }

    private Integer parsePercent(String discountAmount) {
        if (discountAmount == null || discountAmount.isBlank()) return null;
        try {
            String digits = discountAmount.replaceAll("[^\\d]", "");
            return digits.isEmpty() ? null : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Long unixTimestamp) {
        if (unixTimestamp == null) return null;
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(unixTimestamp),
                ZoneId.systemDefault()
        );
    }

    private String computeHash(VkPost post) {
        String raw = post.getText() != null ? post.getText().trim() : "";
        if (raw.isBlank()) {
            return String.valueOf(post.getId());
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }
}