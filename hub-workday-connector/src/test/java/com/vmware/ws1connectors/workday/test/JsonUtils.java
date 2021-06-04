/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vmware.ws1connectors.workday.web.resources.WorkdayResource;
import com.vmware.ws1connectors.workday.test.exception.SerializerException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtils {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).disable(
                    DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

    public static <T> WorkdayResource<T> convertToWorkdayResourceFromJson(final String jsonData, final Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(jsonData, TypeFactory.defaultInstance().constructParametricType(WorkdayResource.class, type));
        } catch (IOException e) {
            throw new SerializerException(e);
        }
    }

    public static <T> T convertFromJsonFile(final String fileName, final Class<T> type) {
        try (InputStream inputStream = new ClassPathResource(fileName).getInputStream()) {
            return OBJECT_MAPPER.readValue(inputStream, type);
        } catch (IOException e) {
            throw new SerializerException(e);
        }
    }

    public static String convertToJson(final Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (IOException e) {
            throw new SerializerException(e);
        }
    }

}
