/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.web;


import com.vmware.connectors.common.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * Created by harshas on 8/8/18.
 */
@RestController
public class ConnectorRootController {

    private final String connectorMetadata;
    private final long maxAge;
    private final TimeUnit unit;

    @Autowired
    public ConnectorRootController(String connectorMetadata,
                                   @Value("${rootDiscovery.cacheControl.maxAge:1}") long maxAge,
                                   @Value("${rootDiscovery.cacheControl.unit:HOURS}") TimeUnit unit) {
        this.connectorMetadata = connectorMetadata;
        this.maxAge = maxAge;
        this.unit = unit;
    }

    @GetMapping(path = "/")
    public ResponseEntity<String> getMetadata(HttpServletRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(maxAge, unit))
                .body(this.connectorMetadata.replace("${CONNECTOR_HOST}", CommonUtils.buildConnectorUrl(request, null)));
    }
}
