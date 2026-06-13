package com.snag.price.parser.api.parser;

import com.snag.price.parser.api.prop.VkProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Service
public class VKGroupHalyavshikiParser extends AbstractVkParser{

    public VKGroupHalyavshikiParser(RestClient restClient,
                                    ObjectMapper mapper,
                                    VkProperties props, DiscountExtractor
                                            extractor) {
        super(restClient, mapper, props, extractor);
    }

    @Override
    public String getName() {
        return "halyavshiki";
    }

    @Override
    public String getDomain() {
        return "halyavshiki";
    }
}
