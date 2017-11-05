/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Rob Worsnop on 10/27/16.
 */
public class Link {
    private final String href;

    @JsonCreator
    public Link(@JsonProperty("href") String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }
}
