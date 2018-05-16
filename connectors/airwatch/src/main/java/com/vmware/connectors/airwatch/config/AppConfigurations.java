/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by harshas on 01/31/18.
 */
@ConfigurationProperties(prefix = "airwatch", ignoreInvalidFields = true)
@Component
public class AppConfigurations {

    @Valid
    @NotNull(message = "Please provide details of all managed apps via a config YML file.")
    private List<AppConfiguration> apps;

    public List<AppConfiguration> getApps() {
        return apps;
    }

    public void setApps(List<AppConfiguration> apps) {
        this.apps = apps;
    }
}
