/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.service;

import org.springframework.core.env.Environment;

import java.util.Locale;

/**
 * Created by harshas on 9/19/17.
 */
public class AppConfigService {

    private final Environment environment;

    public AppConfigService(Environment environment) {
        this.environment = environment;
    }

    public String getAppId(String platform, String appName) {
        return this.environment.getProperty((platform + "." + appName).toLowerCase(Locale.ENGLISH));
    }
}
