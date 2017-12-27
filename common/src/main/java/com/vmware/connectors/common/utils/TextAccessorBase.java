/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.utils;

import com.vmware.connectors.common.context.ContextHolder;
import org.springframework.context.MessageSource;

import java.util.Locale;

public class TextAccessorBase {
    protected final MessageSource messageSource;

    public TextAccessorBase(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    protected static Locale locale() {
        return (Locale) ContextHolder.getContext().get("locale");
    }

    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, locale());
    }
}
