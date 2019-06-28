/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Payload for card requests
 *
 * @author Rob Worsnop
 */
public class CardRequest {

    private final Map<String, Set<String>> tokens;

    private final Map<String, String> config;

    @JsonCreator
    public CardRequest(@JsonProperty("tokens") Map<String, Set<String>> tokens,
                       @JsonProperty("config") Map<String, String> config) {
        this.tokens = tokens == null ? Map.of() : Map.copyOf(tokens);
        this.config = config == null ? Map.of() : Map.copyOf(config);
    }

    /**
     * Returns the metadata tokens extracted by a client.
     * <p>
     * For example:
     * {
     * "emailAddress" : ["rworsnop@vmware.com", "fred@s1.com"],
     * "account" : ["123", "abc", "xyz"]
     * }
     *
     * @return the metadata
     */
    @JsonProperty("tokens")
    public Map<String, Set<String>> getTokens() {
        return tokens;
    }

    /**
     * Returns the tokens from the request, given a key
     * @param key The key to use for lookup
     * @return the tokens for the key
     */
    public Set<String> getTokens(String key) {
        return tokens.get(key);
    }

    /**
     * Returns a value for the specified key from the tokenMap if and only if the map contains a mapping for that key
     * to a set containing exactly one value. If the set contains multiple values or zero values, or if no mapping
     * exists for the key, returns the default value.
     *
     * @param key          The key for which to retrieve the value
     * @param defaultValue The default value to be returned if no single value is retrieved from the map
     * @return the single value for the provided key in the provided map
     */
    public String getTokenSingleValue(String key, String defaultValue) {
        Set<String> values = tokens.get(key);
        if (values != null && values.size() == 1) {
            return values.iterator().next();
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns a value for the specified key from the tokenMap if and only if the map contains a mapping for that key
     * to a set containing exactly one value. If the set contains multiple values or zero values, or if no mapping
     * exists for the key, returns null.
     *
     * @param key The key for which to retrieve the value
     * @return The single value for the provided key in the provided map
     */
    public String getTokenSingleValue(String key) {
        return getTokenSingleValue(key, null);
    }

    /**
     * Returns the connector parameters config map.
     * <p>
     * For example:
     * {
     *     "Discount Percentage": "Discount_Percentage__c",
     *     "Reason for Discount": "Reason_for_Discount__c"
     * }
     *
     * @return the connector parameters config
     */
    public Map<String, String> getConfig() {
        return this.config;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
