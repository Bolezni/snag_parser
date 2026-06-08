package com.snag.price.parser.api.controller;


import com.snag.price.parser.api.dto.VkPost;
import com.snag.price.parser.api.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/discount")
@CrossOrigin(origins = "*")
public class DiscountController {

    private final DiscountService discountService;

    @GetMapping("/page")
    public ResponseEntity<Page<VkPost>> getPageDiscounts(@PageableDefault(direction = Sort.Direction.ASC) Pageable pageable) {
        Page<VkPost> page = discountService.getPageDiscounts(pageable);
        return ResponseEntity.ok(page);
    }
}
