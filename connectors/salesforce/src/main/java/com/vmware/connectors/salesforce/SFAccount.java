/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.salesforce;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * This class is a Memento encapsulating the things we need to know about a Salesforce account.
 * Instances of this class are immutable.
 */
public class SFAccount {

    private final String id;
    private final String name;
    private final List<SFOpportunity> opportunities;

    SFAccount(String id, String name) {
        this.id = id;
        this.name = name;
        this.opportunities = ImmutableList.of();
    }

    SFAccount(SFAccount account, List<SFOpportunity> opportunities) {
        this.id = account.getId();
        this.name = account.getName();
        this.opportunities = ImmutableList.copyOf(opportunities);
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    /**
     * Returns an immutable list of salesforce account's opportunities.
     */
    List<SFOpportunity> getAccOpportunities() {
        return opportunities;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
