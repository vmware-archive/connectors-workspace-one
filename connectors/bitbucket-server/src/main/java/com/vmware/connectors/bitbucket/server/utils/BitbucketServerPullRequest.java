/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.bitbucket.server.utils;

import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Fields related to stash/bitbucket server pull request.
 */
public class BitbucketServerPullRequest {

    // Name of the bitbucket project.
    private final String projectKey;

    // Name of the bitbucket user.
    private final String userKey;

    // Name of the stash repository.
    private final String repositorySlug;

    // Stash pull request identifier.
    private final String pullRequestId;

    public BitbucketServerPullRequest(String userKey, String projectKey, String repositorySlug, String pullRequestId) {
        this.userKey = userKey;
        this.projectKey = projectKey;
        this.repositorySlug = repositorySlug;
        this.pullRequestId = pullRequestId;
    }

    public boolean isProject() {
        return projectKey != null;
    }

    public boolean isUser() {
        return userKey != null;
    }

    public String getUserKey() {
        return userKey;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getRepositorySlug() {
        return repositorySlug;
    }

    public String getPullRequestId() {
        return pullRequestId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
