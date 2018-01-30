/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.bitbucket.server.utils;

public final class BitBucketServerConstants {

    public static final String BITBUCKET_PR_URL_FORMAT = "%s/rest/api/1.0/projects/%s/repos/%s/pull-requests/%s";

    public static final String BITBUCKET_ACTIVITIES_URL_FORMAT = "%s/rest/api/1.0/projects/%s/repos/%s/pull-requests/%s/activities";

    public static final String BITBUCKET_PR_COMMENT_URL_FORMAT = BITBUCKET_PR_URL_FORMAT + "/comments";

    public static final String BITBUCKET_PR_ACTION_FORMAT = BITBUCKET_PR_URL_FORMAT + "/%s?version=%s";

    public static final String BITBUCKET_PR_EMAIL_SUBJECT = "pr_email_subject";

    public static final String BITBUCKET_PR_EMAIL_SUBJECT_REGEX = "(([a-zA-Z0-9]+)\\/([a-zA-Z0-9-]+) - Pull request #([0-9]+):[ ])";

    public static final String BITBUCKET_CLIENT_ACTION_URL = "%s/api/v1/%s/%s/%s/%s";

    // Authorization header for bitbucket server.
    public static final String AUTH_HEADER = "x-bitbucket-server-authorization";

    // BitBucket Server base URL.
    public static final String BASE_URL_HEADER = "x-bitbucket-server-base-url";

    // Routing prefix for bitbucket server connector.
    public static final String ROUTING_PREFIX = "x-routing-prefix";

    public static final String COMMENT_PARAM_KEY = "comment";

    // To prevent CSRF check by BitBucket Server.
    public static final String ATLASSIAN_TOKEN = "X-Atlassian-Token";

    private BitBucketServerConstants() {

    }
}
