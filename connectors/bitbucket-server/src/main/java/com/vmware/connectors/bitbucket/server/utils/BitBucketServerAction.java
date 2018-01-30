/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.bitbucket.server.utils;

public enum BitBucketServerAction {

    /**
     * Retrieve all the comments from bitbucket server pull request.
     */
    ACTIVITES("activities"),

    /**
     * Approve a bitbucket server pull request.
     */
    APPROVE("approve"),

    /**
     * Comment on a bitbucket server pull request.
     */
    COMMENTS("comments"),

    /**
     * Decline an open bitbucket server pull request.
     */
    DECLINE("decline"),

    /**
     * Merge an approved bitbucket server pull request.
     */
    MERGE("merge");

    private String action;

    BitBucketServerAction(final String action) {
        this.action = action;
    }

    public String getAction() {
        return this.action;
    }
}