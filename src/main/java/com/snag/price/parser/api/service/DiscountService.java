package com.snag.price.parser.api.service;

import com.snag.price.parser.api.dto.SaveResult;
import com.snag.price.parser.api.dto.VkPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DiscountService {
    SaveResult saveNewDiscounts(List<VkPost> discounts);

    Page<VkPost> getPageDiscounts(Pageable pageable);
}
