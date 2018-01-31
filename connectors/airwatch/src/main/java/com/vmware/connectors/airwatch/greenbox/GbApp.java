/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.greenbox;

/**
 * Created by harshas on 01/31/18.
 */
public class GbApp {

    private String name;

    private String installLink;

    public GbApp(String name, String installLink) {
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
}
