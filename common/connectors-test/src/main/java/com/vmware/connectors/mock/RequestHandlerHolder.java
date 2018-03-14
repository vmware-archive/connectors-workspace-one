/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.mock;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class RequestHandlerHolder implements RequestHandler {
    private RequestHandler delegate;

    @Override
    public ClientHttpResponse handle(ClientHttpRequest request) throws IOException {
        return delegate.handle(request);
    }

    public void set(RequestHandler requestHandler) {
        this.delegate = requestHandler;
    }
}
