/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.salesforce;

import com.google.common.collect.ImmutableList;
import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

import java.util.List;

/**
 * This class is a Memento encapsulating the things we need to know about a Salesforce account.
 * Instances of this class are immutable.
 */
@AutoProperty
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
        return Pojomatic.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return Pojomatic.hashCode(this);
    }

    @Override
    public String toString() {
        return Pojomatic.toString(this);
    }

}
