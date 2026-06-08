package com.snag.price.parser.api.parser;

import com.snag.price.parser.api.dto.VkPost;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DiscountExtractor {

    private static final List<String> KEYWORDS = List.of(
            "скидк", "промокод", "promo", "акци", "%", "бесплатно",
            "sale", "скидка", "купон", "выгод", "распродаж"
    );

    public List<VkPost> filterDiscountPosts(List<VkPost> posts) {
        return posts.stream()
                .filter(p -> containsDiscount(p.getText()))
                .collect(Collectors.toList());
    }

    public Optional<String> extractPromoCode(String text) {
        Pattern withContext = Pattern.compile(
                "(?i)(?:промокод|promo[- ]?code|код)[:\\s]+([A-Z0-9_-]{3,20})"
        );
        Matcher m = withContext.matcher(text);
        if (m.find()) return Optional.of(m.group(1).toUpperCase());

        Pattern fallback = Pattern.compile("\\b([A-Z0-9]{4,15})\\b");
        Matcher m2 = fallback.matcher(text);
        if (m2.find()) return Optional.of(m2.group(1));

        return Optional.empty();
    }

    public Optional<Integer> extractPercent(String text) {
        Pattern pattern = Pattern.compile("(\\d{1,2})\\s*%");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) return Optional.of(Integer.parseInt(matcher.group(1)));
        return Optional.empty();
    }

    private boolean containsDiscount(String text){
        if(text == null) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return KEYWORDS.stream().anyMatch(lower::contains);
    }
}
