/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class represents a field in the body of a "hero card".
 * Instances of this class are immutable.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.LinguisticNaming")
public class CardBodyField {
    @JsonProperty("type")
    private String type;
    @JsonProperty("title")
    private String title;
    @JsonProperty("description")
    private String description;
    @JsonProperty("content")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<Map<String, String>> content;

    // Don't instantiate directly, use the Builder class below
    private CardBodyField() {
        content = new ArrayList<>();
    }

    /**
     * Get the field's type, a string that tells the client how to render this field.
     *
     * @return The field's type
     */
    public String getType() {
        return type;
    }

    /**
     * Get the field's title, which might be used as a row header in a tabular display of fields.
     *
     * @return The field's title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the field's description.
     *
     * @return The field's description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the content of this field, which can consist of arbitrary JSON objects. The client will use the field's
     * <code>type</code> attribute to determine how to parse the content.
     *
     * @return An unmodifiable copy of the field's content
     */
    public List<Map<String, String>> getContent() {
        return Collections.unmodifiableList(content);
    }

    /**
     * This class allows the construction of CardBodyField objects. To use, create a Builder instance, call its methods
     * to populate the CardBodyField, and call build() to receive the completed CardBodyField and reset the builder.
     * <p>
     * A CardBodyField can be discarded during creation, returning the Builder to its initial state, by calling reset().
     * The build() method calls reset() internally.
     */
    public static class Builder {
        private CardBodyField field;

        /**
         * Create a new Builder instance.
         */
        public Builder() {
            this.field = new CardBodyField();
        }

        /**
         * Discard the CardBodyField currently under construction and return the Builder to its initial state.
         */
        public void reset() {
            this.field = new CardBodyField();
        }

        /**
         * Set the type of the CardBodyField under construction.
         *
         * @param type the CardBodyField's type
         * @return this Builder instance, for method chaining
         */
        public Builder setType(String type) {
            field.type = type;
            return this;
        }

        /**
         * Set the type of the CardBodyField under construction.
         *
         * @param type the CardBodyField's type
         * @return this Builder instance, for method chaining
         */
        public Builder setType(CardBodyFieldType type) {
            field.type = type.name();
            return this;
        }

        /**
         * Set the title of the CardBodyField under construction.
         *
         * @param title the CardBodyField's title
         * @return this Builder instance, for method chaining
         */
        public Builder setTitle(String title) {
            field.title = title;
            return this;
        }

        /**
         * Set the description of the CardBodyField under construction.
         *
         * @param desc the CardBodyField's description
         * @return this Builder instance, for method chaining
         */
        public Builder setDescription(String desc) {
            field.description = desc;
            return this;
        }

        /**
         * Add a new object to the field's content. Content items are stored in the order in which they are added.
         *
         * @param item the object to be added to the field's content
         * @return this Builder instance, for method chaining
         */
        public Builder addContent(Map<String, String> item) {
            field.content.add(Collections.unmodifiableMap(item));
            return this;
        }

        /**
         * Return the CardBodyField under construction and reset the Builder to its initial state.
         *
         * @return The completed CardBodyField
         */
        @SuppressWarnings("PMD.UnnecessaryLocalBeforeReturn")
        public CardBodyField build() {
            CardBodyField completedField = field;
            reset();
            return completedField;
        }
    }
}
