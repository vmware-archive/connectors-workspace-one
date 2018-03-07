/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.context;

import com.google.common.net.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Locale;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by Rob Worsnop on 3/31/17.
 */
class ContextInterceptorTest {
    private ContextInterceptor contextInterceptor = new ContextInterceptor();

    @BeforeEach
    void setup() {
        ContextHolder.getContext().clear();
    }

    @Test
    void testPreHandleWithLanguage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en");

        contextInterceptor.preHandle(request, null, null);

        assertThat(ContextHolder.getContext().get("locale"), equalTo(new Locale("en")));
    }

    @Test
    void testPreHandleWithLocale() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US");

        contextInterceptor.preHandle(request, null, null);

        assertThat(ContextHolder.getContext().get("locale"), equalTo(new Locale("en", "US")));
    }

    @Test
    void testPreHandleWithPrioritizedLocale() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-GB;q=0.2,en-US;q=0.4");

        contextInterceptor.preHandle(request, null, null);

        assertThat(ContextHolder.getContext().get("locale"), equalTo(new Locale("en", "US")));
    }

    @Test
    void testPreHandleWithInvalidHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "just wrong");

        contextInterceptor.preHandle(request, response, null);

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    void testPreHandleWithoutAcceptLanguage() throws Exception {
        contextInterceptor.preHandle(new MockHttpServletRequest("GET", "/"), null, null);

        assertThat(ContextHolder.getContext().get("locale"), is(nullValue()));
    }

    @Test
    void testPostHandle() throws Exception {
        ContextHolder.getContext().put("foo", "bar");

        contextInterceptor.postHandle(null, null, null, null);

        assertThat(ContextHolder.getContext().size(), equalTo(0));
    }
}
