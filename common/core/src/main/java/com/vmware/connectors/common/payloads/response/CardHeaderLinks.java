/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.connectors.common.utils.HashUtil;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardHeaderLinks {

    private final String title;
    private final List<String> subtitle;

    @JsonCreator
    public CardHeaderLinks(
            @JsonProperty("title") String title,
            @JsonProperty("subtitle") List<String> subtitle
    ) {
        this.title = title;
        this.subtitle = subtitle;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("subtitle")
    public List<String> getSubtitle() {
        return subtitle;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

    public String hash() {
        return HashUtil.hash(
                "title: ", this.title,
                "subtitle: ", HashUtil.hashList(subtitle)
        );
    }

}
