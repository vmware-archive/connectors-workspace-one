/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.mock;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

public class MockResponseBuilder {

    private final MockResponse mockResponse = new MockResponse();

    public MockResponseBuilder status(HttpStatus status) {
        mockResponse.setStatus("HTTP/1.1 " + status.value() + " " + status.getReasonPhrase());
        return this;
    }

    public MockResponseBuilder contentType(MediaType contentType) {
        return header(CONTENT_TYPE, contentType);
    }

    public MockResponseBuilder header(String name, Object value) {
        mockResponse.setHeader(name, value);
        return this;
    }

    public MockResponseBuilder body(Resource body) throws IOException {
        mockResponse.setBody(IOUtils.toString(body.getInputStream(), "UTF-8"));
        return this;
    }

    public void enqueue(MockWebServer mock) {
        mock.enqueue(mockResponse);
    }
}
