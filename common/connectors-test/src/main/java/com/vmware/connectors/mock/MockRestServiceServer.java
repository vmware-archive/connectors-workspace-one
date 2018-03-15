/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.mock;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.*;

import java.io.IOException;

public class MockRestServiceServer {
    private final RequestExpectationManager expectationManager;

    private MockRestServiceServer(RequestExpectationManager expectationManager) {
        this.expectationManager = expectationManager;
    }

    public static Builder bindTo(RequestHandlerHolder requestHandlerHolder) {
        return new Builder(requestHandlerHolder);
    }

    public void verify() {
        this.expectationManager.verify();
    }

    public void reset() {
        this.expectationManager.reset();
    }

    public ResponseActions expect(RequestMatcher matcher) {
        return expect(ExpectedCount.once(), matcher);
    }

    public ResponseActions expect(ExpectedCount count, RequestMatcher matcher) {
        return this.expectationManager.expectRequest(count, matcher);
    }

    public static class Builder {
        private final RequestHandlerHolder requestHandlerHolder;
        private boolean ignoreExpectOrder; //NOPMD same name as method because we're copying a Spring class

        public Builder(RequestHandlerHolder requestHandlerHolder) {
            this.requestHandlerHolder = requestHandlerHolder;
        }

        public Builder ignoreExpectOrder(boolean ignoreExpectOrder) {
            this.ignoreExpectOrder = ignoreExpectOrder;
            return this;
        }

        public MockRestServiceServer build() {
            if (this.ignoreExpectOrder) {
                return build(new UnorderedRequestExpectationManager());
            }
            else {
                return build(new SimpleRequestExpectationManager());
            }
        }

        public MockRestServiceServer build(RequestExpectationManager expectationManager) {
            MockRestServiceServer server = new MockRestServiceServer(expectationManager);
            requestHandlerHolder.set(new RequestHandler() {
                @Override
                public ClientHttpResponse handle(ClientHttpRequest request) throws IOException {
                    return expectationManager.validateRequest(request);
                }
            });
            return server;
        }
    }
}
