/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.stash.utils;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class StashComment {

    @JsonProperty("text")
    private String text;

    public StashComment() {
        // Empty constructor.
    }

    public StashComment(final String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
