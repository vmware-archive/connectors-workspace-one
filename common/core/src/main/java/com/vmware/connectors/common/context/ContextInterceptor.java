/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.context;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;

/**
 * For every request, derive the locale from the Accept-Language header and place it on the context
 * <p>
 * Created by Rob Worsnop on 3/31/17.
 */
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class ContextInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            String acceptLanguage = request.getHeader(ACCEPT_LANGUAGE);
            if (StringUtils.isNotBlank(acceptLanguage)) {
                // Parse language ranges from header and store them in priority order
                List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguage);
                // Create locale from the highest priority range
                Locale locale = Locale.forLanguageTag(ranges.get(0).getRange());
                ContextHolder.getContext().put("locale", locale);
            }
            return true;
        } catch (IllegalArgumentException e) {
            response.sendError(400, "Invalid Accept-Language header");
            return false;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        ContextHolder.getContext().clear();
    }
}
