package com.snag.price.parser.api.controller;

import com.snag.price.parser.api.dto.SaveResult;
import com.snag.price.parser.api.dto.VkPost;
import com.snag.price.parser.api.parser.VkWallParser;
import com.snag.price.parser.api.service.DiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vk")
@RequiredArgsConstructor
public class VkWallParserController {

    private final VkWallParser vkWallParser;
    private final DiscountService discountService;

    /**
     * GET /api/vk/parse
     * Запускает парсер и возвращает посты в JSON
     */
    @GetMapping("/parse")
    public ResponseEntity<?> testVkParser() {
        try {
            List<VkPost> posts = vkWallParser.parserAll();

            SaveResult result = discountService.saveNewDiscounts(posts);

            return ResponseEntity.ok(Map.of(
                    "parsed",  posts.size(),
                    "saved",   result.saved(),
                    "skipped", result.skipped(),
                    "preview", posts.stream().limit(5).toList()
            ));

        } catch (Exception e) {
            log.error("Ошибка: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/vk/raw
     * Возвращает сырой JSON от VK — полезно для отладки структуры
     */
    @GetMapping("/raw")
    public ResponseEntity<String> testVkRaw() {
        try {
            // Делаем один запрос и возвращаем чистый JSON от VK
            List<?> batch = vkWallParser.fetchBatch(0);
            return ResponseEntity.ok(batch.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}