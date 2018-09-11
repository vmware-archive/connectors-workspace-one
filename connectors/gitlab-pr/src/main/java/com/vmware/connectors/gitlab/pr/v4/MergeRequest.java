/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.gitlab.pr.v4;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * https://docs.gitlab.com/ee/api/merge_requests.html
 */
@SuppressWarnings("PMD.ExcessivePublicCount")
public class MergeRequest {

    @JsonProperty("sha")
    private String sha;

    @JsonProperty("author")
    private User author;

    @JsonProperty("state")
    private State state;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("merge_status")
    private MergeStatus mergeStatus;

    @JsonProperty("user_notes_count")
    private int userNotesCount;

    @JsonProperty("changes_count")
    private String changesCount;

    @JsonProperty("web_url")
    private String webUrl;


     public static class User {

        @JsonProperty("username")
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
        }

    }

    public enum State {
        opened,
        closed,
        merged;

        public boolean isOpen() {
            return this == opened;
        }
    }

    public enum MergeStatus {
        can_be_merged,
        cannot_be_merged,
        unchecked;

        public boolean canBeMerged() {
            return this == can_be_merged;
        }
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public MergeStatus getMergeStatus() {
        return mergeStatus;
    }

    public void setMergeStatus(MergeStatus mergeStatus) {
        this.mergeStatus = mergeStatus;
    }

    public int getUserNotesCount() {
        return userNotesCount;
    }

    public void setUserNotesCount(int userNotesCount) {
        this.userNotesCount = userNotesCount;
    }

    public String getChangesCount() {
        return changesCount;
    }

    public void setChangesCount(String changesCount) {
        this.changesCount = changesCount;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
