/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.greenbox;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

import java.net.URI;

/**
 * Created by harshas on 01/31/18.
 */
@AutoProperty
public class GreenBoxConnection {

    private final URI baseUri;

    private final String eucToken;

    private final String csrfToken;

    public GreenBoxConnection(URI baseUri, String eucToken, String csrfToken) {
        this.baseUri = baseUri;
        this.eucToken = eucToken;
        this.csrfToken = csrfToken;
    }

    public URI getBaseUrl() {
        return baseUri;
    }

    public String getEucToken() {
        return eucToken;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    @Override
    public String toString() {
        return Pojomatic.toString(this);
    }
}
