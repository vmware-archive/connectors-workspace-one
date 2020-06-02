/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.discovery;

import com.vmware.ws1connectors.workday.exceptions.DiscoveryMetaDataReadFailedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.CONTEXT_PATH_TEMPLATE;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@Slf4j
public class DiscoveryService {

    @Value("classpath:static/discovery/metadata.json")
    private Resource metaDataResource;
    @Autowired ServerProperties serverProperties;

    public String getDiscoveryMetaData() {
        try (InputStream inputStream = metaDataResource.getInputStream()) {
            final String appContextPath = serverProperties.getServlet().getContextPath();
            final Map<String, String> paramMap = Collections.singletonMap(CONTEXT_PATH_TEMPLATE, appContextPath);
            final StringSubstitutor stringSubstitutor = new StringSubstitutor(paramMap);
            return stringSubstitutor.replace(IOUtils.toString(inputStream, UTF_8));
        } catch (IOException ioe) {
            LOGGER.error("Error occurred in reading discovery meta data", ioe);
            throw new DiscoveryMetaDataReadFailedException(ioe);
        }
    }
}
