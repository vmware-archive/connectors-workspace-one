/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.bitbucket.server.utils;

public final class BitbucketServerConstants {

    public static final String BITBUCKET_PR_EMAIL_SUBJECT = "pr_email_subject";

    public static final String BITBUCKET_PR_EMAIL_SUBJECT_REGEX = "(([a-zA-Z0-9]+)\\/([a-zA-Z0-9-]+) - Pull request #([0-9]+):[ ])";

    // Authorization header for bitbucket server.
    public static final String AUTH_HEADER = "X-Connector-Authorization";

    // Bitbucket Server base URL.
    public static final String BASE_URL_HEADER = "X-Connector-Base-Url";

    // Routing prefix for Bitbucket server connector.
    public static final String ROUTING_PREFIX = "x-routing-prefix";

    public static final String COMMENT_PARAM_KEY = "comment";

    // To prevent CSRF check by Bitbucket Server.
    public static final String ATLASSIAN_TOKEN = "X-Atlassian-Token";

    private BitbucketServerConstants() {

    }
}
