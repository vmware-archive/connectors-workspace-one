/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.connectors.common.utils.HashUtil;
import org.springframework.http.HttpMethod;

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

    @JsonProperty("vendor_attachment_url")
    private String vendorAttachmentUrl;

    @JsonProperty("attachment_content_type")
    private String attachmentContentType;

    @JsonProperty("attachment_content_length")
    private Long attachmentContentLength;

    @JsonProperty("attachment_url")
    private String attachmentUrl;

    @JsonProperty("attachment_method")
    private HttpMethod attachmentMethod;

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
     * Get the card body field item attachment name.
     *
     * @return Attachment name
     */
    public String getAttachmentName() {
        return attachmentName;
    }

    /**
     * Get the card body field item's deep link for the attachment in the vendor's system.
     *
     * @return The card body field item vendor attachment url.
     */
    public String getVendorAttachmentUrl() {
        return vendorAttachmentUrl;
    }

    /**
     * Get the card body field item attachment body content type.
     *
     * @return Attachment content type
     */
    public String getAttachmentContentType() {
        return attachmentContentType;
    }

    /**
     * Get the card body field item attachment body content length.
     *
     * @return Attachment content length
     */
    public Long getAttachmentContentLength() {
        return attachmentContentLength;
    }

    /**
     * Get the card body field item's attachment url to stream the attachment through Mobile Flows.
     *
     * @return Attachment url
     */
    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    /**
     * Get the card body field item's attachment method to indicate which HTTP method to use to stream the attachment through Mobile Flows.
     *
     * @return attachment method
     */
    public HttpMethod getAttachmentMethod() {
        return attachmentMethod;
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
         * Set the card body field item's deep link for the attachment in the vendor's system.
         *
         * @param vendorAttachmentUrl the CardBodyFieldItem's vendor attachment url
         * @return this Builder instance, for method chaining
         */
        public Builder setVendorAttachmentUrl(String vendorAttachmentUrl) {
            item.vendorAttachmentUrl = vendorAttachmentUrl;
            return this;
        }

        /**
         * Set the card body field item attachment content type.
         *
         * @param attachmentContentType the CardBodyFieldItem's attachment content type
         * @return this Builder instance, for method chaining
         */
        public Builder setAttachmentContentType(String attachmentContentType) {
            item.attachmentContentType = attachmentContentType;
            return this;
        }

        /**
         * Set the card body field item attachment content length.
         *
         * @param attachmentContentLength the CardBodyFieldItem's attachment content length
         * @return this Builder instance, for method chaining
         */
        public Builder setAttachmentContentLength(Long attachmentContentLength) {
            item.attachmentContentLength = attachmentContentLength;
            return this;
        }

        /**
         * Set the card body field item's attachment url to stream the attachment through Mobile Flows.
         *
         * @param attachmentUrl the CardBodyFieldItem's attachment url.
         * @return this Builder instance, for method chaining
         */
        public Builder setAttachmentUrl(String attachmentUrl) {
            item.attachmentUrl = attachmentUrl;
            return this;
        }

        /**
         * Set the card body field item attachment method.
         *
         * @param attachmentMethod the CardBodyFieldItem's attachment method.
         * @return this Builder instance, for method chaining
         */
        public Builder setAttachmentMethod(HttpMethod attachmentMethod) {
            item.attachmentMethod = attachmentMethod;
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
     * 1. vendor_attachment_url - since it will keep changing for some service providers like Concur for the same attachment.
     * 2. created_at - since timestamp fields are not reliable for hash calculation.
     * 3. updated_at - since timestamp fields are not reliable for hash calculation.
     */
    public String hash() {
        return HashUtil.hash(
                "type:", this.type.name(),
                "title:", this.title,
                "description:", this.description,
                "attachmentName:", this.attachmentName,
                "contentType:", this.attachmentContentType,
                "contentLength:", this.attachmentContentLength,
                "action_url:", this.attachmentUrl,
                "action_type:", this.attachmentMethod == null ? null : this.attachmentMethod.name()
        );
    }
}
