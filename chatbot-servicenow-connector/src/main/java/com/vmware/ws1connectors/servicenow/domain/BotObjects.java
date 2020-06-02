/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class BotObjects {

    @JsonProperty("objects")
    private final List<Map<String, BotItem>> objects;

    public List<Map<String, BotItem>> getObjects() {
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

        public Builder addObject(BotItem object) {
            Map<String, BotItem> botObjectMap = new HashMap<>();
            botObjectMap.put(ServiceNowConstants.ITEM_DETAILS, object);
            objectResults.objects.add(botObjectMap);
            return this;
        }

        public BotObjects build() {
            BotObjects builtObject = objectResults;
            reset();
            return builtObject;
        }
    }
}
