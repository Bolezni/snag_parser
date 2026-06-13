package com.snag.price.parser.api.parser;

import com.snag.price.parser.api.dto.ParsedPost;

import java.util.List;

public interface Parser<T extends ParsedPost> {

    String getName();

    String getDomain();

    List<T> parserAll() throws InterruptedException;

    List<T> fetchBatch(int offset);

    List<T> parserPost(String json);
}
