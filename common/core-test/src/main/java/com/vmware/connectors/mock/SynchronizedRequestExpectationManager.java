/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.mock;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.RequestExpectationManager;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseActions;

import java.io.IOException;

@SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel") // Synchronized methods are fine in this case
public class SynchronizedRequestExpectationManager implements RequestExpectationManager {
    private final RequestExpectationManager delegate;

    public SynchronizedRequestExpectationManager(RequestExpectationManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized ResponseActions expectRequest(ExpectedCount count, RequestMatcher requestMatcher) {
        return delegate.expectRequest(count, requestMatcher);
    }

    @Override
    public synchronized void verify() {
        delegate.verify();
    }

    @Override
    public synchronized void reset() {
        delegate.reset();
    }

    @Override
    public synchronized ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
        return delegate.validateRequest(request);
    }
}
