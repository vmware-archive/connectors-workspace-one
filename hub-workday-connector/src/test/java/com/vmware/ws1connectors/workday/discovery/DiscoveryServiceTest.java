/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.discovery;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.WORKDAY_CONNECTOR_CONTEXT_PATH;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.CONTEXT_PATH_TEMPLATE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
@MockitoSettings
public class DiscoveryServiceTest {
    private static final Resource METADATA_RESOURCE = new ClassPathResource("static/discovery/metadata.json");
    private static final String CONTEXT_PATH_PLACE_HOLDER = "${" + CONTEXT_PATH_TEMPLATE + "}";
    @InjectMocks private DiscoveryService discoveryService;
    @Mock ServerProperties mockServerProperties;

    @BeforeEach public void setup() {
        setField(discoveryService, "metaDataResource", METADATA_RESOURCE);
    }

    private void mockContextPathForServerProperties() {
        final ServerProperties.Servlet mockServlet = mock(ServerProperties.Servlet.class);
        when(mockServerProperties.getServlet()).thenReturn(mockServlet);
        when(mockServlet.getContextPath()).thenReturn(WORKDAY_CONNECTOR_CONTEXT_PATH);
    }

    private String getExpectedDiscoveryMetadata() throws IOException {
        try (InputStream inputStream = METADATA_RESOURCE.getInputStream()) {
            return IOUtils.toString(inputStream, UTF_8).replace(CONTEXT_PATH_PLACE_HOLDER, WORKDAY_CONNECTOR_CONTEXT_PATH);
        }
    }

    @Test public void getsDiscoveryMetadata() throws IOException {
        mockContextPathForServerProperties();
        final String actualMetadata = discoveryService.getDiscoveryMetaData();
        final String expectedMetadata = getExpectedDiscoveryMetadata();
        assertThat(actualMetadata).isEqualTo(expectedMetadata);
    }
}
