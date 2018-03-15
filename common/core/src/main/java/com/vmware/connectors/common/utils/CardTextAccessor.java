/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.utils;

import org.springframework.context.MessageSource;

import java.util.Locale;

public class CardTextAccessor extends TextAccessorBase {

    public CardTextAccessor(MessageSource messageSource) {
        super(messageSource);
    }

    public String getActionLabel(String actionId, Locale locale, Object... args) {
        return messageSource.getMessage(actionId + ".label", args, locale);
    }

    public String getActionCompletedLabel(String actionId, Locale locale, Object... args) {
        return messageSource.getMessage(actionId + ".completedLabel", args, locale);
    }

    public String getHeader(Locale locale, Object... args) {
        return messageSource.getMessage("header", args, locale);
    }

    public String getBody(Locale locale, Object... args) {
        return messageSource.getMessage("body", args, locale);
    }
}
