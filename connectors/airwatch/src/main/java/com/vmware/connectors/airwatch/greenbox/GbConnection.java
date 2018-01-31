/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.greenbox;

import java.net.URI;

/**
 * Created by harshas on 01/31/18.
 */
public class GbConnection {

    private final URI baseUri;

    private String eucToken;

    private String csrfToken;

    public GbConnection(URI baseUri, String eucToken, String csrfToken) {
        System.out.println("eucToken : " + eucToken);
        System.out.println("csrfToken: " + csrfToken);
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
}
