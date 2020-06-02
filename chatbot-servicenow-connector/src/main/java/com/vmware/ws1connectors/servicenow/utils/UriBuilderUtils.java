/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.domain.snow.Task;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SYS_ID;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.TASK_DO;

public final class UriBuilderUtils {
    private static final String BASE_URI_VALIDATE_MSG = "baseUri can't be null";
    private static final String PATH_VALIDATE_MSG = "path can't be null";
    private static final String ARGS_MAP_VALIDATE_MSG = "argumentMap can't be null";
    private static final String CONTEXT_PATH_VALIDATE_MSG = "context path can't be null";
    private static final String REROUTING_URL_VALIDATE_MSG = "rerouting url can't be null";
    private static final String BASE_URL = "BASE_URL";
    private static final String TASK_OBJ = "TASK_OBJ";

    private UriBuilderUtils() { }

    public static String buildUri(URI baseUri, String path, Map<String, String> argumentMap) {
        validateParameters(baseUri, path, argumentMap);
        return UriComponentsBuilder.newInstance().scheme(baseUri.getScheme()).host(baseUri.getHost()).port(baseUri.getPort())
                .path(path).build(argumentMap).toString();
    }

    private static void validateParameters(URI baseUri, String path, Map<String, String> argumentMap) {
        checkNotNull(baseUri, BASE_URI_VALIDATE_MSG);
        checkNotNull(path, PATH_VALIDATE_MSG);
        checkNotNull(argumentMap, ARGS_MAP_VALIDATE_MSG);
    }

    public static String buildUri(URI baseUri, String path) {
        validateParameters(baseUri, path);
        return UriComponentsBuilder.newInstance().scheme(baseUri.getScheme()).host(baseUri.getHost()).port(baseUri.getPort())
                .path(path).build().toUriString();
    }

    private static void validateParameters(URI baseUri, String path) {
        checkNotNull(baseUri, BASE_URI_VALIDATE_MSG);
        checkNotNull(path, PATH_VALIDATE_MSG);
    }

    public static String createConnectorContextUrl(String routingUrl, String contextPath) {
        checkNotNull(contextPath, CONTEXT_PATH_VALIDATE_MSG);
        checkNotNull(routingUrl, REROUTING_URL_VALIDATE_MSG);
        return new StringBuilder(StringUtils.removeEnd(routingUrl, ServiceNowConstants.URL_PATH_SEPERATOR)).append(contextPath).toString();
    }

    public static Link getTaskUrl(String baseUrl, Task task) {
        ArgumentUtils.checkArgumentNotBlank(baseUrl, BASE_URL);
        ArgumentUtils.checkArgumentNotNull(task, TASK_OBJ);
        return new Link(
                UriComponentsBuilder.fromUriString(baseUrl)
                        .replacePath(TASK_DO)
                        .queryParam(SYS_ID, task.getSysId())
                        .build()
                        .toUriString()
        );
    }
}
