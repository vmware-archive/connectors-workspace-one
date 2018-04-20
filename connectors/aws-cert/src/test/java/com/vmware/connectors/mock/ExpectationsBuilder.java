/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.mock;

import okhttp3.Headers;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.CoreMatchers;
import org.springframework.http.HttpMethod;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpMethod.*;

public class ExpectationsBuilder {

    private HttpMethod method;
    private String path;
    private Headers.Builder headersBuilder = new Headers.Builder();
    private String body;

    public ExpectationsBuilder method(HttpMethod method) {
        this.method = method;
        return this;
    }

    public ExpectationsBuilder get() {
        return method(GET);
    }
    public ExpectationsBuilder post() {
        return method(POST);
    }
    public ExpectationsBuilder put() {
        return method(PUT);
    }
    public ExpectationsBuilder delete() {
        return method(DELETE);
    }

    public ExpectationsBuilder path(String path) {
        this.path = path;
        return this;
    }

    public ExpectationsBuilder header(String name, String value) {
        headersBuilder.add(name, value);
        return this;
    }

    public ExpectationsBuilder body(String body) {
        this.body = body;
        return this;
    }

    public void verify(MockWebServer mock) throws InterruptedException {
        RecordedRequest request = mock.takeRequest(30, TimeUnit.SECONDS);
        if (method != null) {
            assertThat(request.getMethod(), equalTo(method.toString()));
        }
        if (path != null) {
            assertThat(request.getPath(), equalTo(path));
        }
        headersBuilder.build().toMultimap().forEach((name, values) ->
                values.forEach(value ->
                    assertThat(request.getHeaders().values(name), CoreMatchers.hasItem(value))
        ));
        if (body != null) {
            assertThat(request.getBody().readString(Charset.defaultCharset()), equalTo(body));
        }
    }
}
