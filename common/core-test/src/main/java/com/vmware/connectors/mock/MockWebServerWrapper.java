/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.mock;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

public class MockWebServerWrapper {
    private final MockWebServer mockWebServer;
    private final RequestExpectationManager expectationManager = new UnorderedRequestExpectationManager();

    public MockWebServerWrapper(MockWebServer mockWebServer) {
        this.mockWebServer = mockWebServer;
        mockWebServer.setDispatcher(dispatcher());
    }

    public ResponseActions expect(RequestMatcher matcher) {
        return expect(ExpectedCount.once(), matcher);
    }

    public ResponseActions expect(ExpectedCount count, RequestMatcher matcher) {
        return this.expectationManager.expectRequest(count, matcher);
    }

    public void verify() {
        expectationManager.verify();
    }

    private Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                 try {
                    return toResponse(expectationManager.validateRequest(toRequest(request)));
                } catch (IOException e) {
                    throw new RuntimeException(e); //NOPMD don't care about tests, thanks
                }
            }
        };
    }

    public void shutdown() throws IOException {
        mockWebServer.shutdown();
    }

    public String url(String path) {
        return mockWebServer.url(path).toString();
    }

    public void reset() {
        expectationManager.reset();
    }

    private static ClientHttpRequest toRequest(RecordedRequest request) throws IOException {
        MockClientHttpRequest clientRequest = new MockClientHttpRequest();
        clientRequest.setMethod(HttpMethod.valueOf(request.getMethod()));
        clientRequest.setURI(URI.create(request.getPath()));
        request.getHeaders().toMultimap().forEach((name, values) ->
                values.forEach(value -> clientRequest.getHeaders().add(name, value)));
        IOUtils.copy(request.getBody().inputStream(), clientRequest.getBody());
        return clientRequest;
    }

    private static MockResponse toResponse(ClientHttpResponse clientResponse) throws IOException {
        MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(clientResponse.getStatusCode().value());
        clientResponse.getHeaders().forEach((name, values) ->
                values.forEach(value -> mockResponse.addHeader(name, value)));
        mockResponse.setBody(IOUtils.toString(clientResponse.getBody(), Charset.defaultCharset()));
        return mockResponse;
    }
}
