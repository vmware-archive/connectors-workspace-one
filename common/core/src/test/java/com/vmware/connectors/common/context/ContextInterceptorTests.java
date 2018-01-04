/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.context;

import com.google.common.net.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Locale;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by Rob Worsnop on 3/31/17.
 */
public class ContextInterceptorTests {
    private ContextInterceptor contextInterceptor = new ContextInterceptor();

    @Before
    public void setup() {
        ContextHolder.getContext().clear();
    }

    @Test
    public void testPreHandleWithLanguage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en");

        contextInterceptor.preHandle(request, null, null);

        assertThat(ContextHolder.getContext().get("locale"), equalTo(new Locale("en")));
    }

    @Test
    public void testPreHandleWithLocale() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US");

        contextInterceptor.preHandle(request, null, null);

        assertThat(ContextHolder.getContext().get("locale"), equalTo(new Locale("en", "US")));
    }

    @Test
    public void testPreHandleWithPrioritizedLocale() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-GB;q=0.2,en-US;q=0.4");

        contextInterceptor.preHandle(request, null, null);

        assertThat(ContextHolder.getContext().get("locale"), equalTo(new Locale("en", "US")));
    }

    @Test
    public void testPreHandleWithInvalidHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "just wrong");

        contextInterceptor.preHandle(request, response, null);

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testPreHandleWithoutAcceptLanguage() throws Exception {
        contextInterceptor.preHandle(new MockHttpServletRequest("GET", "/"), null, null);

        assertThat(ContextHolder.getContext().get("locale"), is(nullValue()));
    }

    @Test
    public void testPostHandle() throws Exception {
        ContextHolder.getContext().put("foo", "bar");

        contextInterceptor.postHandle(null, null, null, null);

        assertThat(ContextHolder.getContext().size(), equalTo(0));
    }
}
