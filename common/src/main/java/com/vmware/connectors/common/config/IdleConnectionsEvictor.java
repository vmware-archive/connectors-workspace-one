

/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.config;

import org.apache.http.nio.conn.NHttpClientConnectionManager;
import org.springframework.scheduling.annotation.Scheduled;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Created by Prabhu Kumar on 6/11/17.
 */

public class IdleConnectionsEvictor {

    private final NHttpClientConnectionManager connMgr;

    public IdleConnectionsEvictor(NHttpClientConnectionManager connMgr) {
        this.connMgr = connMgr;
    }

    @Scheduled(initialDelay = 1000, fixedRate = 20_000)
    public void run() {
        connMgr.closeIdleConnections(1, MINUTES);
    }
}
