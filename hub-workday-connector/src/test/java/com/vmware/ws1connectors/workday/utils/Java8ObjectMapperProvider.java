/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.workday.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.javacrumbs.jsonunit.providers.Jackson2ObjectMapperProvider;

public class Java8ObjectMapperProvider implements Jackson2ObjectMapperProvider {
    private final ObjectMapper mapper;

    private final ObjectMapper lenientMapper;

    public Java8ObjectMapperProvider() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        lenientMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        lenientMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        lenientMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        lenientMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    @Override public ObjectMapper getObjectMapper(boolean lenient) {
        return lenient ? lenientMapper : mapper;
    }
}

