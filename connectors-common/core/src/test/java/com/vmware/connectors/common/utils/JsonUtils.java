/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.connectors.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtils {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    public static <T> T convertFromJsonFile(final String fileName, final Class<T> type) {
        try (InputStream inputStream = new ClassPathResource(fileName).getInputStream()) {
            return OBJECT_MAPPER.readValue(inputStream, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
