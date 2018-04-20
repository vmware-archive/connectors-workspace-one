/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.config;

import com.vmware.connectors.airwatch.exceptions.UnsupportedPlatform;

import java.util.List;

/**
 * Created by harshas on 01/31/18.
 * Supported platforms are 'ios' and 'android'.
 */
public class AppConfiguration {

    private String app;

    private ManagedApp android;

    private ManagedApp ios;

    private List<String> keywords;

    public String getApp() {
        return app;
    }

    public void setApp(String application) {
        this.app = application;
    }

    public ManagedApp getAndroid() {
        return android;
    }

    public void setAndroid(ManagedApp android) {
        this.android = android;
    }

    public ManagedApp getIos() {
        return ios;
    }

    public void setIos(ManagedApp ios) {
        this.ios = ios;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public ManagedApp getApp(String platform) {
        if ("android".equalsIgnoreCase(platform)) {
            return android;
        } else if ("ios".equalsIgnoreCase(platform)) {
            return ios;
        }
        else {
            throw new UnsupportedPlatform(platform + " is not supported. It should be either android or ios.");
        }
    }
}
