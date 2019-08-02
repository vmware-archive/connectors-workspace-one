/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.connectors.common.utils.HashUtil;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

/**
 * Represents an item inside CardBodyField to display a separate section in the hub notification.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.LinguisticNaming")
public class CardBodyFieldItem {

    @JsonProperty("type")
    private CardBodyFieldType type;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("attachment_name")
    private String attachmentName;

    @JsonProperty("attachment_url")
    private String attachmentURL;

    @JsonProperty("attachment_body")
    private byte[] attachmentBody;

    @JsonProperty("content_type")
    private MediaType contentType;

    @JsonProperty("content_length")
    private Long contentLength;

    @JsonProperty("action_url")
    private String actionURL;

    @JsonProperty("action_type")
    private HttpMethod actionType;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    // Use builder class to instantiate the object.
    private CardBodyFieldItem() {
    }

    /**
     * Get the card body field item type, a string that tells the hub client how to render this field.
     *
     * @return The card body field item type
     */
    public CardBodyFieldType getType() {
        return type;
    }

    /**
     * Get the card body field item title.
     *
     * @return Field item title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the card body field item description.
     *
     * @return Field item description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the card body field item attachment name for an expense report.
     *
     * @return Attachment URL
     */
    public String getAttachmentName() {
        return attachmentName;
    }

    /**
     * Get the card body field item attachment url for an expense report.
     *
     * @return The card body field item attachment url.
     */
    public String getAttachmentURL() {
        return attachmentURL;
    }

    /**
     * Get the card body field item attachment body of an expense report.
     *
     * @return Attachment content body
     */
    public byte[] getAttachmentBody() {
        if (this.attachmentBody == null) {
            return null;
        }
        return Arrays.copyOf(attachmentBody, attachmentBody.length);
    }

    /**
     * Get the card body field item attachment body content type.
     *
     * @return Attachment content type
     */
    public MediaType getContentType() {
        return contentType;
    }

    /**
     * Get the card body field item attachment body content length.
     *
     * @return Attachment content length
     */
    public Long getContentLength() {
        return contentLength;
    }

    /**
     * Get the card body field item action url. Action URL is used to fetch the attachment when required by the hub client.
     *
     * @return Action URL.
     */
    public String getActionURL() {
        return actionURL;
    }

    /**
     * Get the card body field item action type. Action type indicates HTTP method used to access the action url.
     *
     * @return Action type.
     */
    public HttpMethod getActionType() {
        return actionType;
    }

    /**
     * Get the card body field item created at.
     *
     * @return Created At.
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Get the card body field item updated at.
     *
     * @return Updated At.
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    public static class Builder {
        private CardBodyFieldItem item;

        /**
         * Create a new Builder instance.
         */
        public Builder() {
            this.item = new CardBodyFieldItem();
        }

        /**
         * Set the card body field item type.
         *
         * @param type the CardBodyFieldItem's type
         * @return this Builder instance, for method chaining
         */
        public Builder setType(CardBodyFieldType type) {
            item.type = type;
            return this;
        }

        /**
         * Set the card body field item description.
         *
         * @param description the CardBodyFieldItem's description
         * @return this Builder instance, for method chaining
         */
        public Builder setDescription(String description) {
            item.description = description;
            return this;
        }

        /**
         * Set the card body field item title.
         *
         * @param title the CardBodyFieldItem's title
         * @return this Builder instance, for method chaining
         */
        public Builder setTitle(String title) {
            item.title = title;
            return this;
        }

        /**
         * Set the card body field item attachment name.
         *
         * @param attachmentName the CardBodyFieldItem's attachment name
         * @return this Builder instance, for method chaining
         */
        public Builder setAttachmentName(String attachmentName) {
            item.attachmentName = attachmentName;
            return this;
        }

        /**
         * Set the card body field item attachment url.
         *
         * @param attachmentURL the CardBodyFieldItem's attachment url
         * @return this Builder instance, for method chaining
         */
        public Builder setAttachmentURL(String attachmentURL) {
            item.attachmentURL = attachmentURL;
            return this;
        }

        /**
         * Set the card body field item attachment body.
         *
         * @param attachmentBody the CardBodyFieldItem's attachment body
         * @return this Builder instance, for method chaining
         */
        @SuppressWarnings("PMD.NullAssignment")
        public Builder setAttachmentBody(byte[] attachmentBody) {
            if (attachmentBody == null) {
                item.attachmentBody = null;
            } else {
                item.attachmentBody = Arrays.copyOf(attachmentBody, attachmentBody.length);
            }
            return this;
        }

        /**
         * Set the card body field item content type.
         *
         * @param contentType the CardBodyFieldItem's content type
         * @return this Builder instance, for method chaining
         */
        public Builder setContentType(MediaType contentType) {
            item.contentType = contentType;
            return this;
        }

        /**
         * Set the card body field item content length.
         *
         * @param contentLength the CardBodyFieldItem's content length
         * @return this Builder instance, for method chaining
         */
        public Builder setContentLength(Long contentLength) {
            item.contentLength = contentLength;
            return this;
        }

        /**
         * Set the card body field item action url.
         *
         * @param actionURL the CardBodyFieldItem's action url.
         * @return this Builder instance, for method chaining
         */
        public Builder setActionURL(String actionURL) {
            item.actionURL = actionURL;
            return this;
        }

        /**
         * Set the card body field item created_at.
         *
         * @param createdAt the CardBodyFieldItem's created at.
         * @return this Builder instance, for method chaining
         */
        public Builder setCreatedAt(Date createdAt) {
            item.createdAt = Optional.ofNullable(createdAt)
                    .map(Date::getTime)
                    .map(Date::new)
                    .orElse(null);
            return this;
        }

        /**
         * Set the card body field item updated_at.
         *
         * @param updatedAt the CardBodyFieldItem's updated at.
         * @return this Builder instance, for method chaining
         */
        public Builder setUpdatedAt(Date updatedAt) {
            item.updatedAt = Optional.ofNullable(updatedAt)
                    .map(Date::getTime)
                    .map(Date::new)
                    .orElse(null);
            return this;
        }

        /**
         * Set the card body field item action type.
         *
         * @param actionType the CardBodyFieldItem's action type.
         * @return this Builder instance, for method chaining
         */
        public Builder setActionType(HttpMethod actionType) {
            item.actionType = actionType;
            return this;
        }

        /**
         * Return the CardBodyFieldItem under construction and reset the Builder to its initial state.
         *
         * @return The completed CardBodyFieldItem
         */
        public CardBodyFieldItem build() {
            CardBodyFieldItem fieldItem = item;
            reset();
            return fieldItem;
        }

        /**
         * Discard the CardBodyFieldItem currently under construction and return the Builder to its initial state.
         */
        private void reset() {
            item = new CardBodyFieldItem();
        }
    }

    /**
     * Note: Hash calculation does not include the below fields
     * 1. attachment_url - since it will keep changing for some service providers like Concur for the same attachment.
     * 2. created_at - since timestamp fields are not reliable for hash calculation.
     * 3. updated_at - since timestamp fields are not reliable for hash calculation.
     */
    public String hash() {
        return HashUtil.hash(
                "type: ", this.type.name(),
                "title: ", this.title,
                "description: ", this.description,
                "attachmentName: ", this.attachmentName,
                "contentType: ", this.contentType == null ? null : this.contentType.toString(),
                "contentLength: ", this.contentLength,
                "action_url: ", this.actionURL,
                "action_type: ", this.actionType == null ? null : this.actionType.name()
        );
    }
}
