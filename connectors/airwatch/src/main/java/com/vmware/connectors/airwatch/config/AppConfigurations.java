/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by harshas on 01/31/18.
 */
@ConfigurationProperties(prefix = "airwatch")
@Component
public class AppConfigurations {

    private List<AppConfiguration> apps;

    public List<AppConfiguration> getApps() {
        return apps;
    }

    public void setApps(List<AppConfiguration> apps) {
        this.apps = apps;
    }
}
