/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.greenbox;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.net.URI;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Created by harshas on 01/31/18.
 */
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
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
