/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@SuppressWarnings("PMD.LinguisticNaming")
public class BotActionUserInput {

    private String id;

    private String label;

    private String format;

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int minLength;

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int maxLength;

    private BotActionUserInput() {

    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getFormat() {
        return format;
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public static class Builder {

        private BotActionUserInput actionUserInput;

        public Builder() {
            actionUserInput = new BotActionUserInput();
        }

        private void reset() {
            actionUserInput = new BotActionUserInput();
        }

        /*
         * The field's ID which will be used as the key when this field is submitted.
         */
        public Builder setId(String id) {
            actionUserInput.id = id;
            return this;
        }

        public Builder setLabel(String label) {
            actionUserInput.label = label;
            return this;
        }

        /*
         * Allowed formats is "textarea" or "select"
         */
        public Builder setFormat(String format) {
            actionUserInput.format = format;
            return this;
        }

        public Builder setMinLength(int minLength) {
            actionUserInput.minLength = minLength;
            return this;
        }

        public Builder setMaxLength(int maxLength) {
            actionUserInput.maxLength = maxLength;
            return this;
        }

        public BotActionUserInput build() {
            BotActionUserInput builtUserInput = actionUserInput;
            reset();
            return builtUserInput;
        }
    }
}
