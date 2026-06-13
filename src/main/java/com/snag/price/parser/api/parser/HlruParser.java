package com.snag.price.parser.api.parser;

import com.snag.price.parser.api.dto.HlruPost;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HlruParser implements Parser<HlruPost> {

    private static final String BASE_URL = "https://hl.ru";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36";

    @Override
    public String getName() {
        return "hlru";
    }

    @Override
    public String getDomain() {
        return "hl.ru";
    }

    @Override
    public List<HlruPost> parserAll() throws InterruptedException {
        List<HlruPost> result = new ArrayList<>();

        // Страницы: /promocodes?page=1, /promocodes?page=2 ...
        for (int page = 1; page <= 10; page++) {
            List<HlruPost> batch = fetchBatch(page);
            if (batch.isEmpty()) break;

            result.addAll(batch);
            log.info("[hl.ru] Страница {}: {} постов", page, batch.size());
            Thread.sleep(1000);
        }

        return result;
    }

    @Override
    public List<HlruPost> fetchBatch(int page) {
        try {
            String url = BASE_URL + "/promocodes?page=" + page;

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept-Language", "ru-RU,ru;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .timeout(12_000)
                    .get();

            return parserPost(doc.html());

        } catch (Exception e) {
            log.error("[hl.ru] Ошибка страницы {}: {}", page, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<HlruPost> parserPost(String html) {
        List<HlruPost> result = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(html);

            // Из твоего HTML: каждая карточка — article.postItemPromoV
            Elements cards = doc.select("article.postItemPromoV");

            if (cards.isEmpty()) {
                log.warn("[hl.ru] Карточки не найдены — возможно SPA, нужен другой подход");
                return result;
            }

            for (Element card : cards) {
                HlruPost post = mapCard(card);
                if (post != null) result.add(post);
            }

        } catch (Exception e) {
            log.error("[hl.ru] Ошибка парсинга HTML: {}", e.getMessage());
        }

        return result;
    }

    private HlruPost mapCard(Element card) {
        HlruPost post = new HlruPost();

        // ── Магазин ──────────────────────────────────────────────────────────
        Element shopName = card.selectFirst("a.postBlock-shop span");
        if (shopName != null) post.setShopName(shopName.text());

        Element shopLogo = card.selectFirst("a.postBlock-shop img");
        if (shopLogo != null) {
            // Берём src или srcSet первый вариант
            String logo = shopLogo.attr("src");
            if (logo.isBlank()) logo = shopLogo.attr("data-src");
            post.setShopLogoUrl(logo);
        }

        // ── Заголовок и ссылка ───────────────────────────────────────────────
        Element titleEl = card.selectFirst("a.postItemPromoV_title");
        if (titleEl == null) return null; // пустая карточка

        post.setTitle(titleEl.text());

        String href = titleEl.attr("href");
        post.setSourceUrl(href.startsWith("http") ? href : BASE_URL + href);
        post.setId(extractId(href));

        // ── Скидка: "-20%", "Бесплатно", "До 3000₽" ─────────────────────────
        Element discountEl = card.selectFirst("a.postItemPromoV_discount div div");
        if (discountEl != null) {
            // Убираем html-комментарии <!--  --> которые Next.js вставляет
            post.setDiscountAmount(discountEl.text().trim());
        }

        // ── Категория: "Промокоды", "Скидки" ─────────────────────────────────
        Element catEl = card.selectFirst("span.postBlock-category");
        if (catEl != null) post.setCategory(catEl.text());

        // ── Рейтинг (температура 12°, -12°) ──────────────────────────────────
        Element ratingEl = card.selectFirst("span.postRating-text");
        if (ratingEl != null) {
            String raw = ratingEl.text()
                    .replace("°", "")
                    .trim();
            try {
                post.setRating(Integer.parseInt(raw));
            } catch (NumberFormatException ignored) {
            }
        }

        return post;
    }

    // ── Утилиты ──────────────────────────────────────────────────────────────

    // Из URL "...slug-297921" → "297921"
    private String extractId(String url) {
        if (url == null || !url.contains("-")) return url;
        String[] parts = url.split("-");
        return parts[parts.length - 1];
    }
}
