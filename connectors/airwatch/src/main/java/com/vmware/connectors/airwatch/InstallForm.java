/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch;

import javax.validation.constraints.NotBlank;

@SuppressWarnings("PMD.MethodNamingConventions")
public class InstallForm {

    @NotBlank
    String appName;

    @NotBlank
    String udid;

    @NotBlank
    String platform ;

    public String getAppName() {
        return appName;
    }

    // https://github.com/spring-projects/spring-framework/issues/18012
    public void setApp_name(String appName) {
        this.appName = appName;
    }

    public String getUdid() {
        return udid;
    }

    public void setUdid(String udid) {
        this.udid = udid;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
