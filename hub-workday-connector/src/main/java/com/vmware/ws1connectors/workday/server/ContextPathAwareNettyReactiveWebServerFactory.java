/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.ContextPathCompositeHandler;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.util.Collections.singletonMap;

@Component
@Slf4j
public class ContextPathAwareNettyReactiveWebServerFactory extends NettyReactiveWebServerFactory {
    @Autowired ServerProperties serverProperties;

    @Override public WebServer getWebServer(HttpHandler httpHandler) {
        final String appContextPath = serverProperties.getServlet().getContextPath();
        Map<String, HttpHandler> handlerMap = singletonMap(appContextPath, httpHandler);
        LOGGER.info("Adding context path {} to the app", appContextPath);
        return super.getWebServer(new ContextPathCompositeHandler(handlerMap));
    }

}
