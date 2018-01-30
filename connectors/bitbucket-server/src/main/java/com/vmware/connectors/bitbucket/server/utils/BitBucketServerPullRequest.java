/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.bitbucket.server.utils;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

/**
 * Fields related to stash/bitbucket server pull request.
 */
@AutoProperty
public class BitBucketServerPullRequest {

    // Name of the stash project.
    private String projectKey;

    // Name of the stash repository.
    private String repositorySlug;

    // Stash pull request identifier.
    private String pullRequestId;

    public BitBucketServerPullRequest() {
        // Empty constructor.
    }

    public BitBucketServerPullRequest(final String projectKey,
                                      final String repositorySlug,
                                      final String pullRequestId) {
        this.projectKey = projectKey;
        this.repositorySlug = repositorySlug;
        this.pullRequestId = pullRequestId;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(final String projectKey) {
        this.projectKey = projectKey;
    }

    public String getRepositorySlug() {
        return repositorySlug;
    }

    public void setRepositorySlug(final String repositorySlug) {
        this.repositorySlug = repositorySlug;
    }

    public String getPullRequestId() {
        return pullRequestId;
    }

    public void setPullRequestId(final String pullRequestId) {
        this.pullRequestId = pullRequestId;
    }

    @Override
    public String toString() {
        return Pojomatic.toString(this);
    }
}
