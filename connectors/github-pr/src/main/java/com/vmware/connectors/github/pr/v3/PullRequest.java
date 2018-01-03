/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.github.pr.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

import java.util.Date;

/**
 * https://developer.github.com/v3/pulls/
 */
@SuppressWarnings("PMD.ExcessivePublicCount")
@AutoProperty
public class PullRequest {

    @JsonProperty("_links")
    private Links links;

    @JsonProperty("head")
    private Head head;

    @JsonProperty("user")
    private User user;

    @JsonProperty("merged_by")
    private User mergedBy;

    @JsonProperty("state")
    private String state;

    @JsonProperty("title")
    private String title;

    @JsonProperty("body")
    private String body;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("closed_at")
    private Date closedAt;

    @JsonProperty("merged_at")
    private Date mergedAt;

    @JsonProperty("merged")
    private boolean merged;

    /*
     * true - this PR can be merged
     * false - this PR cannot be merged (conflicts)
     * null - the process to determine mergeability hasn't completed yet
     */
    @JsonProperty("mergeable")
    private Boolean mergeable;

    @JsonProperty("comments")
    private int comments;

    @JsonProperty("review_comments")
    private int reviewComments;

    @JsonProperty("commits")
    private int commits;

    @JsonProperty("additions")
    private int additions;

    @JsonProperty("deletions")
    private int deletions;

    @JsonProperty("changed_files")
    private int changedFiles;


    @AutoProperty
    public static class Links {

        @JsonProperty("html")
        private HRef html;

        @JsonProperty("issue")
        private HRef issue;

        @JsonProperty("comments")
        private HRef comments;

        @JsonProperty("review_comments")
        private HRef reviewComments;

        public HRef getHtml() {
            return html;
        }

        public void setHtml(HRef html) {
            this.html = html;
        }

        public HRef getIssue() {
            return issue;
        }

        public void setIssue(HRef issue) {
            this.issue = issue;
        }

        public HRef getComments() {
            return comments;
        }

        public void setComments(HRef comments) {
            this.comments = comments;
        }

        public HRef getReviewComments() {
            return reviewComments;
        }

        public void setReviewComments(HRef reviewComments) {
            this.reviewComments = reviewComments;
        }

        @Override
        public String toString() {
            return Pojomatic.toString(this);
        }

    }

    @AutoProperty
    public static class Head {

        @JsonProperty("sha")
        private String sha;

        public String getSha() {
            return sha;
        }

        public void setSha(String sha) {
            this.sha = sha;
        }

        @Override
        public String toString() {
            return Pojomatic.toString(this);
        }

    }

    @AutoProperty
    public static class User {

        @JsonProperty("login")
        private String login;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        @Override
        public String toString() {
            return Pojomatic.toString(this);
        }

    }


    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    public Head getHead() {
        return head;
    }

    public void setHead(Head head) {
        this.head = head;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getMergedBy() {
        return mergedBy;
    }

    public void setMergedBy(User mergedBy) {
        this.mergedBy = mergedBy;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }

    public Date getMergedAt() {
        return mergedAt;
    }

    public void setMergedAt(Date mergedAt) {
        this.mergedAt = mergedAt;
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    /*
     * true - this PR can be merged
     * false - this PR cannot be merged (conflicts)
     * null - the process to determine mergeability hasn't completed yet
     */
    public Boolean getMergeable() {
        return mergeable;
    }

    /*
     * true - this PR can be merged
     * false - this PR cannot be merged (conflicts)
     * null - the process to determine mergeability hasn't completed yet
     */
    public void setMergeable(Boolean mergeable) {
        this.mergeable = mergeable;
    }

    public int getComments() {
        return comments;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }

    public int getReviewComments() {
        return reviewComments;
    }

    public void setReviewComments(int reviewComments) {
        this.reviewComments = reviewComments;
    }

    public int getCommits() {
        return commits;
    }

    public void setCommits(int commits) {
        this.commits = commits;
    }

    public int getAdditions() {
        return additions;
    }

    public void setAdditions(int additions) {
        this.additions = additions;
    }

    public int getDeletions() {
        return deletions;
    }

    public void setDeletions(int deletions) {
        this.deletions = deletions;
    }

    public int getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(int changedFiles) {
        this.changedFiles = changedFiles;
    }

    @Override
    public String toString() {
        return Pojomatic.toString(this);
    }

}
