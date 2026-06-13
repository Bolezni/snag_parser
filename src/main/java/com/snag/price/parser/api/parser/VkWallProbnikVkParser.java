package com.snag.price.parser.api.parser;

import com.snag.price.parser.api.prop.VkProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Service
public class VkWallProbnikVkParser extends AbstractVkParser {


    public VkWallProbnikVkParser(RestClient restClient,
                                 DiscountExtractor extractor,
                                 VkProperties props,
                                 ObjectMapper mapper) {
        super(restClient, mapper, props, extractor);
    }

    @Override
    public String getName() {
        return "probnick";
    }

    @Override
    public String getDomain() {
        return "probnick_ru";
    }

}
