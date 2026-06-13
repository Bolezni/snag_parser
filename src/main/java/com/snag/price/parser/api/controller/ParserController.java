package com.snag.price.parser.api.controller;

import com.snag.price.parser.api.dto.ParsedPost;
import com.snag.price.parser.api.dto.SaveResult;
import com.snag.price.parser.api.parser.ParserRegistry;
import com.snag.price.parser.api.service.DiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/parser")
@RequiredArgsConstructor
public class ParserController {

    private final ParserRegistry registry;
    private final DiscountService discountService;

    /** Запустить все парсеры */
    @PostMapping("/run")
    public ResponseEntity<?> runAll() {
        new Thread(() -> {
            try {
                List<ParsedPost> posts = registry.runAll();
                discountService.saveNewDiscounts(posts);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        return ResponseEntity.ok(Map.of("status", "started", "parsers", registry.getNames()));
    }

    /** Запустить конкретный парсер: POST /api/parser/run/probnick */
    @PostMapping("/run/{name}")
    public ResponseEntity<?> runOne(@PathVariable String name) throws InterruptedException {
        List<ParsedPost> posts = registry.runOne(name);
        //Page<ParsedPost> page = Page
        SaveResult result = discountService.saveNewDiscounts(posts);
        return ResponseEntity.ok(Map.of(
                "parser", name,
                "parsed", posts.size(),
                "saved", result.saved(),
                "skipped", result.skipped()
        ));
    }

    /** Список доступных парсеров */
    @GetMapping("/list")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(registry.getNames());
    }
}