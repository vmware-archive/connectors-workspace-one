/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.bitbucket.server.utils;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class BitBucketServerComment {

    @JsonProperty("text")
    private String text;

    public BitBucketServerComment() {
        // Empty constructor.
    }

    public BitBucketServerComment(final String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
