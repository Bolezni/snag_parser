package com.snag.price.parser.api.dto;

public record SaveResult(
        int saved,
        int skipped
) {
}
