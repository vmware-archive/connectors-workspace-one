package com.vmware.connectors.common.utils;

import com.vmware.connectors.common.payloads.response.Card;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

/**
 * Common utility functions to be used across connectors parent.
 */
public final class CommonUtils {

    private static final String DEFAULT_IMAGE_PATH = "/images/connector.png";

    private CommonUtils() {
        // Utility class.
    }

    public static void buildConnectorImageUrl(final Card.Builder card, final HttpServletRequest request) {
        final String uri = buildConnectorImageUrl(request);

        if (StringUtils.isNotBlank(uri)) {
            card.setImageUrl(uri);
        }
    }

    public static String buildConnectorImageUrl(final HttpRequest request) {
        return buildConnectorImageUrl(request, DEFAULT_IMAGE_PATH);
    }

    public static String buildConnectorImageUrl(final HttpServletRequest request) {
        return buildConnectorImageUrl(request, DEFAULT_IMAGE_PATH);
    }

    public static String buildConnectorImageUrl(final HttpRequest request, final String path) {
        return UriComponentsBuilder.fromHttpRequest(request).replacePath(path).build().toString();
    }

    public static String buildConnectorImageUrl(final HttpServletRequest request, final String path) {
        return buildConnectorImageUrl(new ServletServerHttpRequest(request), path);
    }

}
