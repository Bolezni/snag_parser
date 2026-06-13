package com.snag.price.parser.api.parser;


import com.snag.price.parser.api.dto.ParsedPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ParserRegistry {

    private final Map<String, Parser<ParsedPost>> parsers;

    @SuppressWarnings("unchecked")
    public ParserRegistry(List<Parser<? extends ParsedPost>> parserList) {
        this.parsers = parserList.stream()
                .collect(Collectors.toMap(Parser::getName, p -> (Parser<ParsedPost>) p));
        log.info("Add new parse: {}", parsers.keySet());
    }

    /** Запустить все — возвращает общий тип */
    public List<ParsedPost> runAll() throws InterruptedException {
        List<ParsedPost> all = new ArrayList<>();
        for (Parser<ParsedPost> parser : parsers.values()) {
            log.info("→ Запуск: {}", parser.getName());
            all.addAll(parser.parserAll()); // Теперь скомпилируется без ошибок!
        }
        return all;
    }

    public List<ParsedPost> runOne(String name) throws InterruptedException {
        Parser<ParsedPost> parser = parsers.get(name);
        if (parser == null) throw new IllegalArgumentException("Парсер не найден: " + name);
        return new ArrayList<>(parser.parserAll());
    }


    public List<String> getNames() {
        return List.copyOf(parsers.keySet());
    }
}
