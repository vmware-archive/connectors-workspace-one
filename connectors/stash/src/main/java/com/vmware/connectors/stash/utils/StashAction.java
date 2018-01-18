/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.stash.utils;

public enum StashAction {

    // Approve a stash pull request.
    APPROVE("approve"),

    // Comment on a stash pull request.
    COMMENTS("comments"),

    // Decline an open stash pull request.
    DECLINE("decline"),

    // Merge an approved stash pull request.
    MERGE("merge");

    private String action;

    StashAction(final String action) {
        this.action = action;
    }

    public String getAction() {
        return this.action;
    }
}