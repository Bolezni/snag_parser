package com.snag.price.parser.api.parser;

import com.snag.price.parser.api.dto.VkPost;

import java.util.List;

public interface Parser {
    List<?> parserAll() throws InterruptedException;
    List<VkPost> parserAll(java.util.function.Predicate<VkPost> stopCondition) throws InterruptedException;
    List<?> fetchBatch(int offset);
    List<?>  parserPost(String json);
}
