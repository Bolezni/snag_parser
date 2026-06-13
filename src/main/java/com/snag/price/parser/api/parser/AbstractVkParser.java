package com.snag.price.parser.api.parser;


import com.snag.price.parser.api.dto.VkPost;
import com.snag.price.parser.api.prop.VkProperties;
import lombok.extern.slf4j.Slf4j;
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
public abstract class AbstractVkParser implements Parser<VkPost> {

    protected final RestClient restClient;
    protected final ObjectMapper mapper;
    protected final VkProperties props;
    protected final DiscountExtractor extractor;

    private static final Pattern DISCOUNT_PATTERN =
            Pattern.compile("(-?\\d{1,2})\\s*%");
    private static final Pattern PROMO_PATTERN =
            Pattern.compile("(?i)(?:промокод|promo[- ]?code|код)[:\\s]+([A-ZА-Яa-zа-я0-9_-]{3,20})");

    protected AbstractVkParser(RestClient restClient,
                               ObjectMapper mapper,
                               VkProperties props,
                               DiscountExtractor extractor) {
        this.restClient = restClient;
        this.mapper = mapper;
        this.props = props;
        this.extractor = extractor;
    }


    @Override
    public List<VkPost> parserAll() throws InterruptedException {
        List<VkPost> all = new ArrayList<>();

        for (int i = 0; i < props.getMaxRequests(); i++) {
            int offset = i * props.getPostsPerRequest();
            List<VkPost> batch = fetchBatch(offset);

            if (batch.isEmpty()) break;

            all.addAll(batch);
            log.info("[{}] Загружено {} постов (offset={})", getName(), all.size(), offset);
            Thread.sleep(400);
        }

        List<VkPost> discountPosts = filterAndEnrich(all);
        log.info("[{}] Постов всего: {}, со скидками: {}", getName(), all.size(), discountPosts.size());
        return discountPosts;
    }

    @Override
    public List<VkPost> fetchBatch(int offset) {
        try {
            String domain = getDomain();
            String json = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/method/wall.get")
                            .queryParam("domain", domain)
                            .queryParam("count", props.getPostsPerRequest())
                            .queryParam("offset", offset)
                            .queryParam("filter", "owner")
                            .queryParam("access_token", props.getToken())
                            .queryParam("v", "5.199")
                            .build())
                    .retrieve()
                    .body(String.class);

            return parserPost(json);
        } catch (Exception e) {
            log.error("[{}] Ошибка запроса (offset={}): {}", getName(), offset, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<VkPost> parserPost(String json) {
        List<VkPost> result = new ArrayList<>();
        if (json == null) return result;

        try {
            JsonNode root = mapper.readTree(json);

            if (root.has("error")) {
                int code = root.path("error").path("error_code").asInt();
                log.error("[{}] VK API error {}: {}", getName(), code,
                        root.path("error").path("error_msg").asString());
                if (code == 6 || code == 9) Thread.sleep(3000);
                return result;
            }

            JsonNode items = root.path("response").path("items");
            if (!items.isArray()) return result;

            for (JsonNode item : items) {
                VkPost post = mapPost(item);
                if (post != null) result.add(post);
            }

        } catch (Exception e) {
            log.error("[{}] Ошибка парсинга JSON: {}", getName(), e.getMessage());
        }

        return result;
    }

    /**
     * Фильтрация и обогащение постов.
     * Переопредели если нужна специфичная логика для группы.
     */
    protected List<VkPost> filterAndEnrich(List<VkPost> posts) {
        List<VkPost> filtered = extractor.filterDiscountPosts(posts);
        filtered.forEach(post -> {
            extractor.extractPromoCode(post.getText()).ifPresent(post::setPromoCode);
            extractor.extractPercent(post.getText())
                    .ifPresent(pct -> post.setDiscountAmount("-" + pct + "%"));
        });
        return filtered;
    }

    private VkPost mapPost(JsonNode n) {
        if (n.path("marked_as_ads").asInt(0) == 1) return null;
        if (n.has("copy_history")) return null;

        String text = n.path("text").asString("").trim();

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
        if (pct.find()) post.setDiscountAmount("-" + pct.group(1) + "%");

        Matcher promo = PROMO_PATTERN.matcher(text);
        if (promo.find()) post.setPromoCode(promo.group(1).trim().toUpperCase());

        text.lines().map(String::trim).filter(l -> !l.isBlank())
                .findFirst().ifPresent(post::setProductName);
    }

    private String extractImage(JsonNode post) {
        JsonNode attachments = post.path("attachments");
        if (!attachments.isArray()) return null;

        for (JsonNode att : attachments) {
            if (!"photo".equals(att.path("type").asString())) continue;
            JsonNode sizes = att.path("photo").path("sizes");
            if (!sizes.isArray() || sizes.isEmpty()) continue;

            Map<String, String> urlByType = new HashMap<>();
            for (JsonNode size : sizes) {
                urlByType.put(size.path("type").asString(), size.path("url").asString());
            }
            for (String type : new String[]{"w", "z", "y", "x"}) {
                if (urlByType.containsKey(type)) return urlByType.get(type);
            }
            return sizes.get(sizes.size() - 1).path("url").asString();
        }
        return null;
    }

    private String buildPostUrl(JsonNode n) {
        long ownerId = Math.abs(n.path("owner_id").asLong());
        long postId = n.path("id").asLong();
        return "https://vk.com/" + getDomain() + "?w=wall-" + ownerId + "_" + postId;
    }


}
