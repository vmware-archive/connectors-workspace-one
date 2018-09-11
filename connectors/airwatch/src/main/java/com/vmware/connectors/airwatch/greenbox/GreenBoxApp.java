/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.greenbox;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Created by harshas on 01/31/18.
 */
public class GreenBoxApp {

    private String name;

    private String installLink;

    public GreenBoxApp(String name, String installLink) {
        this.name = name;
        this.installLink = installLink;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setInstallStatus(String installLink) {
        this.installLink = installLink;
    }

    public String getName() {
        return name;
    }

    public String getInstallLink() {
        return installLink;
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
