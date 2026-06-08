package com.snag.price.parser.api.scheduler;

import com.snag.price.parser.api.dto.VkPost;
import com.snag.price.parser.api.parser.VkWallParser;
import com.snag.price.parser.api.service.DiscountService;
import com.snag.price.parser.model.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParserScheduler {

    private final VkWallParser vkParser;
    private final DiscountService discountService;
    private final DiscountRepository discountRepository;// твой сервис сохранения

    @Scheduled(cron = "0 0 3 * * *") // Каждые 30 минут
    public void runParsingPipeline() {
        try {
            // Мы вызываем метод И передаем туда условие остановки.
            // Как только внутри парсера stopCondition наткнется на пост,
            // ID которого уже есть в базе данных, метод вернет true, и цикл в ВК прервется.
            List<VkPost> discountsOnly = vkParser.parserAll(post ->
                    discountRepository.existsByExternalId(String.valueOf(post.getId()))
            );

            if (discountsOnly.isEmpty()) {
                log.info("Свежих постов со скидками нет.");
                return;
            }

            discountService.saveNewDiscounts(discountsOnly);

        } catch (InterruptedException e) {
            log.error("Парсинг прерван");
            Thread.currentThread().interrupt();
        }
    }
}
