package com.vmware.connectors.common.utils;

import com.vmware.connectors.common.payloads.response.Card;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

/**
 * Common utility functions to be used across connectors parent.
 */
public final class CommonUtils {

    public static final String IMAGE_PATH = "/images/connector.png";

    private CommonUtils() {
        // Singleton class.
    }

    public static void buildImageUrl(final Card.Builder card, final HttpServletRequest request) {
        if (StringUtils.isBlank(request.getHeader(HttpHeaders.HOST))) {
            return;
        }
        final UriComponentsBuilder uriComponents = UriComponentsBuilder.newInstance();
        uriComponents.host(request.getHeader(HttpHeaders.HOST));
        uriComponents.scheme(request.getScheme());
        uriComponents.path(IMAGE_PATH);

        card.setImageUrl(uriComponents.build().toString());
    }
}
