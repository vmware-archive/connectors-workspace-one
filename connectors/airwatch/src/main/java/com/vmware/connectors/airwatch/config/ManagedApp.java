/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.config;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

import javax.validation.constraints.NotNull;

/**
 * Created by harshas on 01/31/18.
 */
@AutoProperty
public class ManagedApp {

    @NotNull(message = "App name should be provided for the platform.")
    private String name;

    @NotNull(message = "App id should be provided for the platform.")
    private String id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
