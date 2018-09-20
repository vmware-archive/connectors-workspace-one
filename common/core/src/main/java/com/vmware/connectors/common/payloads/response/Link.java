/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

public class Link {
    private final String href;

    @JsonCreator
    public Link(@JsonProperty("href") String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
