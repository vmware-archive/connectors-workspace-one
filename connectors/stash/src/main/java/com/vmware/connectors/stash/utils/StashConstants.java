/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.stash.utils;

public final class StashConstants {

    public static final String STASH_PR_URL_FORMAT = "%s/rest/api/1.0/projects/%s/repos/%s/pull-requests/%s";

    public static final String STASH_PR_COMMENT_URL_FORMAT = STASH_PR_URL_FORMAT + "/comments";

    public static final String STASH_PR_ACTION_FORMAT = STASH_PR_URL_FORMAT + "/%s?version=%s";

    public static final String STASH_PR_EMAIL_SUBJECT = "pr_email_subject";

    public static final String STASH_PR_EMAIL_SUBJECT_REGEX = "(([a-zA-Z0-9]+)\\/([a-zA-Z0-9-]+) - Pull request #([0-9]+): [a-zA-Z0-9- .\\\\()#$&@+=_|!]*)";

    public static final String STASH_CLIENT_ACTION_URL = "%s/api/v1/%s/%s/%s/%s";

    public static final String COMMENT_PARAM_KEY = "comment";

    // To prevent CSRF check by stash.
    public static final String ATLASSIAN_TOKEN = "X-Atlassian-Token";

    private StashConstants() {

    }
}
