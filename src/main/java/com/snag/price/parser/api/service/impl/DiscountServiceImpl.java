package com.snag.price.parser.api.service.impl;

import com.snag.price.parser.api.dto.SaveResult;
import com.snag.price.parser.api.dto.VkPost;
import com.snag.price.parser.api.service.DiscountService;
import com.snag.price.parser.model.Discount;
import com.snag.price.parser.model.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.snag.price.parser.mapper.DiscountMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository repository;
    private final DiscountMapper mapper;

    @Override
    public SaveResult saveNewDiscounts(List<VkPost> posts) {
        int saved   = 0;
        int skipped = 0;

        for (VkPost post : posts) {
            try {
                boolean wasSaved = saveOne(post);
                if (wasSaved) saved++;
                else skipped++;
            } catch (Exception e) {
                log.warn("Не удалось сохранить пост id={}: {}", post.getId(), e.getMessage());
                skipped++;
            }
        }

        log.info("Сохранено: {}, пропущено дублей: {}", saved, skipped);
        return new SaveResult(saved, skipped);
    }

    public boolean saveOne(VkPost post) {
        if (repository.existsByExternalId(String.valueOf(post.getId()))) {
            return false;
        }

        Discount discount = mapper.toDiscount(post);

        if (repository.existsByContentHash(discount.getContentHash())) {
            return false;
        }

        repository.save(discount);
        return true;
    }

    @Override
    public Page<VkPost> getPageDiscounts(Pageable pageable) {
        Page<Discount> page = repository.findAll(pageable);

        return page.map(mapper::toVkPost);
    }
}
