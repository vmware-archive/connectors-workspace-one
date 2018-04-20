/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.service;

import com.vmware.connectors.airwatch.config.AppConfiguration;
import com.vmware.connectors.airwatch.config.AppConfigurations;
import com.vmware.connectors.airwatch.config.ManagedApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Created by harshas on 9/19/17.
 */
public class AppConfigService {

    private static final Logger logger = LoggerFactory.getLogger(AppConfigService.class);

    private final AppConfigurations appConfigurations;

    public AppConfigService(AppConfigurations appConfigurations) {
        this.appConfigurations = appConfigurations;
    }

    public Optional<ManagedApp> findManagedApp(String keyword, String platform) {
        for (AppConfiguration appConfiguration : appConfigurations.getApps()) {
            ManagedApp app = appConfiguration.getApp(platform);

            if (app == null) {
                logger.warn("{} app is not configured for {} platform.", appConfiguration.getApp(), platform);
                continue;
            }

            // If the keyword matches to the app's name or its configured keywords.
            if (app.getName().equalsIgnoreCase(keyword) ||
                    appConfiguration.getKeywords().stream().anyMatch(keyword::equalsIgnoreCase)) {
                return Optional.of(app);
            }
        }
        return Optional.empty();
    }
}
