/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class represents the header of a "hero card", which can contain a title and/or a subtitle.
 * Instances of this class are immutable.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardHeader {

    private final String title;
    private final List<String> subtitle;

    /**
     * Create a new CardHeader.
     * <p>
     * If a header lacks a title or subtitle, it is recommended to supply <code>null</code> for the missing
     * values rather than, e.g., an empty string.
     *
     * @param title The title
     * @param subtitle The subtitle(s)
     */
    @JsonCreator
    public CardHeader(@JsonProperty("title") String title,
                      @JsonProperty("subtitle") List<String> subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getSubtitle() {
        return subtitle;
    }
}
