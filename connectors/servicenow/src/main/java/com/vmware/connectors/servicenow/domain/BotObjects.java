/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class BotObjects {

    @JsonProperty("objects")
    private final List<BotItem> objects;

    public List<BotItem> getObjects() {
        return objects;
    }

    private BotObjects() {
        this.objects = new ArrayList<>();
    }

    public static class Builder {

        private BotObjects objectResults;

        public Builder() {
            objectResults = new BotObjects();
        }

        private void reset() {
            objectResults = new BotObjects();
        }

        public Builder setObject(BotItem object) {
            objectResults.objects.add(object);
            return this;
        }

        public BotObjects build() {
            BotObjects builtObject = objectResults;
            reset();
            return builtObject;
        }
    }
}
