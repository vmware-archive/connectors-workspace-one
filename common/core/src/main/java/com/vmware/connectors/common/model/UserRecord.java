/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;


@AutoProperty
public class UserRecord {

    @JsonProperty("name")
    private String name;
    @JsonProperty("email")
    private String emailAddress;

    public String getName() {
        return name;
    }

    @JsonIgnore
    public String getFirstName() {
        if (name == null) {
            return null;
        } else if (name.contains(",")) {
            return name.substring(name.lastIndexOf(',') + 1).trim();
        } else {
            return name.substring(0, name.indexOf(' ')).trim();
        }
    }

    @JsonIgnore
    public String getLastName() {
        if (name == null) {
            return null;
        } else if (name.contains(",")) {
            return name.substring(0, name.lastIndexOf(',')).trim();
        } else {
            return name.substring(name.lastIndexOf(' ') + 1).trim();
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String toString() {
        return Pojomatic.toString(this);
    }

    @Override
    public boolean equals(Object other) {
        return Pojomatic.equals(this, other);
    }

    @Override
    public int hashCode() {
        return Pojomatic.hashCode(this);
    }
}
