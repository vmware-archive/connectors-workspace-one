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

/**
 * This class represents the header of a "hero card", which can contain a title and/or a subtitle.
 * Instances of this class are immutable.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardHeader {

    private final String title;
    private final List<String> subtitle;
    private final CardHeaderLinks links;

    /**
     * Create a new CardHeader.
     * <p>
     * If a header lacks a title or subtitle, it is recommended to supply <code>null</code> for the missing
     * values rather than, e.g., an empty string.
     *
     * @param title    The title
     * @param subtitle The subtitle(s)
     */
    @JsonCreator
    public CardHeader(@JsonProperty("title") String title,
                      @JsonProperty("subtitle") List<String> subtitle,
                      @JsonProperty("links") CardHeaderLinks links) {
        this.title = title;
        this.subtitle = subtitle;
        this.links = links;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("subtitle")
    public List<String> getSubtitle() {
        return subtitle;
    }

    @JsonProperty("links")
    public CardHeaderLinks getLinks() {
        return links;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

    public String hash() {
        return HashUtil.hash(
                "title: ", this.title,
                "subtitle: ", HashUtil.hashList(subtitle),
                "links: ", links == null ? null : links.hash()
        );
    }
}
