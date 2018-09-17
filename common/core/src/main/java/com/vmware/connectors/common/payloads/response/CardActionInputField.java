/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class represents a user-supplied parameter that will be sent with the request fired by the client when the
 * parent CardAction is triggered.
 * <p>
 * Instances of this class are unmodifiable once created. The CardActionInputField class cannot be directly constructed;
 * use the CardActionInputField.Builder class to create and populate a CardActionInputField instance.
 */
@SuppressWarnings("PMD.LinguisticNaming")
public class CardActionInputField {

    @JsonProperty("id")
    private String id;

    @JsonProperty("label")
    private String label;

    @JsonProperty("format")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String format;

    @JsonProperty("options")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Map<String, String> options;

    @JsonProperty("min_length")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int minLength;

    @JsonProperty("max_length")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int maxLength;

    // Do not instantiate directly.
    private CardActionInputField() {
        this.options = new LinkedHashMap<>();
    }

    /**
     * Get the field's ID, which will be used as the key when this field is submitted.
     *
     * @return The field's ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get the field's label, which will be displayed to the user to describe the expected content of the field.
     *
     * @return The field's label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the field's format, which specifies what type of information is represented by the value of the field.
     * The client should verify compliance with this constraint before submitting the request.
     * <p>
     * Valid values are those specified by the latest JSON Schema specification:
     * see http://json-schema.org/latest/json-schema-validation.html#rfc.section.7
     *
     * @return The field's format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Get the minimum acceptable length of the field's value. The client should verify compliance with this
     * constraint before submitting the request.
     * <p>
     * The default value, 0, implies no constraint on minimum length, and the "min_length" field will be
     * omitted from the serialized JSON representation. Any value less than 0 will be interpreted as 0.
     *
     * @return The minimum allowed length of the field's value.
     */
    public int getMinLength() {
        return minLength;
    }

    /**
     * Get the maximum acceptable length of the field's value. The client should verify compliance with this
     * constraint before submitting the request.
     * <p>
     * The default value, 0, implies no constraint on maximum length, and the "max_length" field will be
     * omitted from the serialized JSON representation. Any value less than 0 will be interpreted as 0.
     *
     * @return The minimum allowed length of the field's value.
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Get the field's options, which will be displayed to the user to select related options of the field.
     * The Map returned by this is unmodifiable.
     *
     * @return The field's options
     */
    public Map<String, String> getOptions() {
        return Collections.unmodifiableMap(options);
    }

    /**
     * This class allows the construction of CardActionInputField objects. To use, create a Builder instance,
     * call its methods to populate the CardActionInputField, and call build() to receive the completed
     * CardActionInputField and reset the builder.
     * <p>
     * A CardActionInputField can be discarded during creation, returning the Builder to its initial state,
     * by calling reset(). The build() method calls reset() internally.
     */
    public static class Builder {
        private CardActionInputField inputField;

        /**
         * Create a new Builder instance.
         */
        public Builder() {
            inputField = new CardActionInputField();
        }

        /**
         * Discard the CardActionInputField currently under construction and return the Builder to its initial state.
         */
        public void reset() {
            inputField = new CardActionInputField();
        }

        /**
         * Set the id of the field under construction.
         *
         * @param id The field's ID
         * @return This Builder instance, for method chaining
         */
        public Builder setId(String id) {
            inputField.id = id;
            return this;
        }

        /**
         * Set the label of the field under construction.
         *
         * @param label The field's label
         * @return This Builder instance, for method chaining
         */
        public Builder setLabel(String label) {
            inputField.label = label;
            return this;
        }

        /**
         * Set the format of the field under construction.
         *
         * @param format The field's format
         * @return This Builder instance, for method chaining
         */
        public Builder setFormat(String format) {
            inputField.format = format;
            return this;
        }

        /**
         * Add a new select option related to the field.
         *
         * @param value value of the option
         * @param label label of the option
         * @return This Builder instance, for method chaining
         */
        public Builder addOption(String value, String label) {
            inputField.options.put(value, label);
            return this;
        }

        /**
         * Set the minimum acceptable length of the field under construction. The default value, 0,
         * implies no constraint on minimum length.
         *
         * @param minLength The field's minimum length
         * @return This Builder instance, for method chaining
         */
        public Builder setMinLength(int minLength) {
            inputField.minLength = minLength;
            return this;
        }

        /**
         * Set the maximum acceptable length of the field under construction. The default value, 0,
         * implies no constraint on maximum length.
         *
         * @param maxLength The field's maximum length
         * @return This Builder instance, for method chaining
         */
        public Builder setMaxLength(int maxLength) {
            inputField.maxLength = maxLength;
            return this;
        }

        /**
         * Return the CardActionInputField under construction and reset the Builder to its initial state.
         *
         * @return The completed CardActionInputField
         */
        @SuppressWarnings("PMD.UnnecessaryLocalBeforeReturn")
        public CardActionInputField build() {
            validate(inputField);
            CardActionInputField completedInputField = inputField;
            reset();
            return completedInputField;
        }

        /*
        Check the CardActionInputField for consistency. Fix any errors that are fixable, and report any that are not.
         */
        private boolean validate(CardActionInputField field) {
            if (field.minLength < 0) {
                field.minLength = 0;
            }

            if (field.maxLength < field.minLength) {
                field.maxLength = 0;
            }

            // TODO: throw exception for missing id or label

            return true;
        }
    }
}
