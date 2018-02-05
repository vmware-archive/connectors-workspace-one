/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.greenbox;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

/**
 * Created by harshas on 01/31/18.
 */
@AutoProperty
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
