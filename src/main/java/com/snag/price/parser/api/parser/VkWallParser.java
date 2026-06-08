package com.snag.price.parser.api.parser;

import com.snag.price.parser.api.dto.VkPost;
import com.snag.price.parser.api.prop.VkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VkWallParser implements Parser {

    private final RestClient   restClient;
    private final ObjectMapper mapper;
    private final VkProperties props;
    private final DiscountExtractor discountExtractor;

    // Паттерны для извлечения данных из текста поста
    private static final Pattern DISCOUNT_PATTERN = Pattern.compile(
            "(-?\\d{1,2})\\s*%"
    );
    private static final Pattern PROMO_PATTERN = Pattern.compile(
            "(?i)(?:промокод|promo[- ]?code|код)[:\\s]+([A-ZА-Яa-zа-я0-9_-]{3,20})"
    );

    @Override
    public List<VkPost> parserAll() throws InterruptedException {
        return parserAll(post -> false); // Передаем false, значит парсим всё подряд, без остановки
    }

    @Override
    public List<VkPost> parserAll(java.util.function.Predicate<VkPost> stopCondition) throws InterruptedException {
        List<VkPost> all = new ArrayList<>();
        boolean shouldStop = false;

        for (int i = 0; i < props.getMaxRequests(); i++) {
            int offset = i * props.getPostsPerRequest();
            List<VkPost> batch = fetchBatch(offset);

            if (batch.isEmpty()) break;

            // Поштучно проверяем посты из батча
            for (VkPost post : batch) {
                if (stopCondition.test(post)) {
                    shouldStop = true; // Сигнал, что пора закругляться
                    break;
                }
                all.add(post);
            }

            log.info("Загружено {} постов (offset={})", all.size(), offset);

            if (shouldStop) {
                log.info("Обнаружены старые посты. Прерываем сбор, чтобы не флудить в API ВК.");
                break;
            }

            Thread.sleep(400);
        }

        // Твоя оригинальная логика экстрактора (работает уже со свежесобранным списком all)
        List<VkPost> discountPosts = discountExtractor.filterDiscountPosts(all);
        log.info("Постов всего: {}, со скидками: {}", all.size(), discountPosts.size());

        discountPosts.forEach(post -> {
            String text = post.getText();
            discountExtractor.extractPromoCode(text).ifPresent(post::setPromoCode);
            discountExtractor.extractPercent(text).ifPresent(pct -> post.setDiscountAmount("-" + pct + "%"));
        });

        return discountPosts;
    }

    @Override
    public List<VkPost> fetchBatch(int offset) {
        try {
            String json = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/method/wall.get")
                            .queryParam("domain",       props.getDomain())
                            .queryParam("count",        props.getPostsPerRequest())
                            .queryParam("offset",       offset)
                            .queryParam("filter",       "owner")
                            .queryParam("access_token", props.getToken())
                            .queryParam("v",            "5.199") // актуальная версия
                            .build())
                    .retrieve()
                    .body(String.class);

            return parserPost(json);

        } catch (Exception e) {
            log.error("Ошибка запроса VK API (offset={}): {}", offset, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<VkPost> parserPost(String json) {
        List<VkPost> result = new ArrayList<>();
        if (json == null) return result;

        try {
            JsonNode root = mapper.readTree(json);

            // Проверяем ошибку API
            if (root.has("error")) {
                int errorCode = root.path("error").path("error_code").asInt();
                String errorMsg = root.path("error").path("error_msg").asText();

                log.error("VK API error {}: {}", errorCode, errorMsg);

                // Код 6 — Too many requests, Код 9 — Flood control
                if (errorCode == 6 || errorCode == 9) {
                    log.warn("Сработало ограничение частоты запросов ВК. Активируем backoff-паузу...");
                    try {
                        Thread.sleep(3000); // Спим 3 секунды, давая API «остыть»
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return result;
            }

            JsonNode items = root.path("response").path("items");
            if (!items.isArray()) {
                log.warn("Неожиданная структура ответа VK: {}", json.substring(0, Math.min(200, json.length())));
                return result;
            }

            for (JsonNode item : items) {
                try {
                    VkPost post = mapPost(item);
                    if (post != null) result.add(post);
                } catch (Exception e) {
                    log.warn("Ошибка маппинга поста id={}: {}",
                            item.path("id").asLong(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Ошибка парсинга JSON: {}", e.getMessage());
        }

        return result;
    }


    private VkPost mapPost(JsonNode n) {
        // Пропускаем рекламные посты
        if (n.path("marked_as_ads").asInt(0) == 1) return null;

        // Пропускаем репосты (нет оригинального текста)
        if (n.has("copy_history")) return null;

        String text = n.path("text").asText("").trim();

        VkPost post = new VkPost();
        post.setId(n.path("id").asLong());
        post.setText(text);
        post.setDate(n.path("date").asLong());
        post.setLikes(n.path("likes").path("count").asInt(0));
        post.setReposts(n.path("reposts").path("count").asInt(0));
        post.setSourceUrl(buildPostUrl(n));
        post.setImageUrl(extractImage(n));

        extractDiscountData(post);

        return post;
    }


    private void extractDiscountData(VkPost post) {
        String text = post.getText();
        if (text == null || text.isBlank()) return;

        Matcher pct = DISCOUNT_PATTERN.matcher(text);
        if (pct.find()) {
            post.setDiscountAmount("-" + pct.group(1) + "%");
        }

        Matcher promo = PROMO_PATTERN.matcher(text);
        if (promo.find()) {
            post.setPromoCode(promo.group(1).trim().toUpperCase());
        }

        text.lines()
                .map(String::trim)
                .filter(l -> !l.isBlank())
                .findFirst()
                .ifPresent(post::setProductName);
    }


    private String extractImage(JsonNode post) {
        JsonNode attachments = post.path("attachments");
        if (!attachments.isArray()) return null;

        for (JsonNode att : attachments) {
            if (!"photo".equals(att.path("type").asText())) continue;

            JsonNode sizes = att.path("photo").path("sizes");
            if (!sizes.isArray() || sizes.isEmpty()) continue;

            String[] preferred = {"w", "z", "y", "x"};
            Map<String, String> urlByType = new HashMap<>();
            for (JsonNode size : sizes) {
                urlByType.put(size.path("type").asText(), size.path("url").asText());
            }

            for (String type : preferred) {
                if (urlByType.containsKey(type)) return urlByType.get(type);
            }

            return sizes.get(sizes.size() - 1).path("url").asText();
        }
        return null;
    }


    private String buildPostUrl(JsonNode n) {
        long ownerId = Math.abs(n.path("owner_id").asLong());
        long postId  = n.path("id").asLong();
        return "https://vk.com/" + props.getDomain()
                + "?w=wall-" + ownerId + "_" + postId;
    }
}
