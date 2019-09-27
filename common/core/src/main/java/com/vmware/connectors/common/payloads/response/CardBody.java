/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.connectors.common.utils.HashUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * This class represents the body of a hero card, which can contain a text description, a human-readable
 * timestamp, and zero or more fields represented by CardBodyField objects.
 * <p>
 * Instances of this class are unmodifiable once created. The CardBody class cannot be directly constructed;
 * use the CardBody.Builder class to create and populate a CardBody instance.
 */
@SuppressWarnings("PMD.LinguisticNaming")
public class CardBody {
    @JsonProperty("description")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;


    @JsonProperty("fields")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<CardBodyField> fields;

    // Don't instantiate directly - use the Builder class below
    private CardBody() {
        fields = new ArrayList<>();
    }

    /**
     * Get the description of the body. This might typically be rendered as plain body text near the top of the card.
     *
     * @return The description text
     */
    public String getDescription() {
        return description;
    }


    /**
     * Get the fields of the body, if any. Fields are rendered differently according to their <code>type</code>
     * attribute; consult your client documentation for details.
     *
     * @return An unmodifiable list of the fields in the body, in the order they were added
     */
    public List<CardBodyField> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * This class allows the construction of CardBody objects. To use, create a Builder instance, call its methods
     * to populate the CardBody, and call build() to receive the completed CardBody and reset the builder.
     * <p>
     * A CardBody can be discarded during creation, returning the Builder to its initial state, by calling reset().
     * The build() method calls reset() internally.
     */
    public static class Builder {

        private CardBody body;

        /**
         * Create a new Builder instance.
         */
        public Builder() {
            body = new CardBody();
        }

        /**
         * Discard the CardBody currently under construction and return the Builder to its initial state.
         */
        public void reset() {
            body = new CardBody();
        }

        /**
         * Set the description of the CardBody under construction.
         *
         * @param desc the CardBody's description
         * @return this Builder instance, for method chaining
         */
        public Builder setDescription(String desc) {
            body.description = desc;
            return this;
        }

        /**
         * Add a CardBodyField to the CardBody under construction if not null.
         * Fields are stored in insertion order.
         *
         * @param field The CardBodyField to be added
         * @return this Builder instance, for method chaining
         */
        public Builder addField(CardBodyField field) {
            if (field != null) {
                body.fields.add(field);
            }
            return this;
        }

        /**
         * Return the CardBody under construction and reset the Builder to its initial state.
         *
         * @return The completed CardBody
         */
        @SuppressWarnings("PMD.UnnecessaryLocalBeforeReturn")
        public CardBody build() {
            CardBody completedBody = body;
            reset();
            return completedBody;
        }
    }

    public String hash() {
        final List<String> fieldsHashList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fields)) {
            fields.forEach(cardBodyField -> fieldsHashList.add(cardBodyField == null ? StringUtils.SPACE : cardBodyField.hash()));
        }

        return HashUtil.hash(
                "description: ", this.description,
                "fields: ", HashUtil.hashList(fieldsHashList)
        );
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
