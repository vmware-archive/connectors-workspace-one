/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.google.common.io.CharStreams;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.Charsets;
import org.springframework.core.io.ClassPathResource;
import com.vmware.ws1connectors.servicenow.exception.FileUtilsException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtils {

    public static String readFileAsString(final String fileName) {
        try (InputStream inputStream = new ClassPathResource(fileName).getInputStream()) {
            return CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
        } catch (IOException e) {
            throw new FileUtilsException("Failed to read file " + fileName, e);
        }
    }
}
