/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a collection of {@link Card}.
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
public class Cards {
    @JsonProperty("objects")
    private final List<Card> cards = new ArrayList<>();

    /**
     * Gets collection of connector cards
     *
     * @return List of Cards
     */
    public List<Card> getCards() {
        return cards;
    }

}
