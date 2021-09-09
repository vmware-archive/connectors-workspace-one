/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class RequestInfoTest {
    private static final String TENANT_NAME = "vmware_gms";
    private static final String TENANT_URL = "https://impl.workday.com";
    private static final String ROUTING_PREFIX = "https://dev.hero.example.com/connectors/id/";
    private static final Locale LOCALE = Locale.ENGLISH;
    private static final String BASE_URL = "http://workday.com";
    private static final String CONNECTOR_AUTH = "connectorAuth";

    @Test
    void testCardsConfigGetters() {
        RequestInfo requestInfo = RequestInfo.builder().tenantName(TENANT_NAME)
                .tenantUrl(TENANT_URL)
                .baseUrl(BASE_URL)
                .routingPrefix(ROUTING_PREFIX)
                .connectorAuth(CONNECTOR_AUTH)
                .isPreHire(true)
                .locale(LOCALE)
                .build();
        assertForGetters(requestInfo);
    }

    @Test
    void testCardsConfigSetters() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        requestInfo.setBaseUrl(BASE_URL);
        requestInfo.setConnectorAuth(CONNECTOR_AUTH);
        requestInfo.setRoutingPrefix(ROUTING_PREFIX);
        requestInfo.setTenantName(TENANT_NAME);
        requestInfo.setTenantUrl(TENANT_URL);
        requestInfo.setPreHire(true);
        requestInfo.setLocale(LOCALE);
        assertForGetters(requestInfo);
    }

    private void assertForGetters(RequestInfo requestInfo) {
        assertThat(requestInfo.getBaseUrl()).isEqualTo(BASE_URL);
        assertThat(requestInfo.getRoutingPrefix()).isEqualTo(ROUTING_PREFIX);
        assertThat(requestInfo.getConnectorAuth()).isEqualTo(CONNECTOR_AUTH);
        assertThat(requestInfo.getTenantName()).isEqualTo(TENANT_NAME);
        assertThat(requestInfo.getTenantUrl()).isEqualTo(TENANT_URL);
        assertThat(requestInfo.isPreHire()).isTrue();
        assertThat(requestInfo.getLocale()).isEqualTo(LOCALE);
    }
}
