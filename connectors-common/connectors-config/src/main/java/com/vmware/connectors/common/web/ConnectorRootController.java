/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.web;


import com.vmware.connectors.common.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.server.reactive.ServerHttpRequest;


/**
 * Created by harshas on 8/8/18.
 */
@RestController
public class ConnectorRootController {

    private final String connectorMetadata;

    @Autowired
    public ConnectorRootController(String connectorMetadata) {
        this.connectorMetadata = connectorMetadata;
    }

    @GetMapping(path = "/")
    public ResponseEntity<String> getMetadata(ServerHttpRequest request) {
        return ResponseEntity.ok()
                .body(this.connectorMetadata.replace("${CONNECTOR_HOST}", CommonUtils.buildConnectorUrl(request, null)));
    }
}
