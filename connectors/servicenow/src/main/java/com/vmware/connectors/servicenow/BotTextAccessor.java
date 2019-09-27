/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow;

import com.vmware.connectors.common.utils.TextAccessorBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class BotTextAccessor extends TextAccessorBase {

    @Autowired
    public BotTextAccessor(MessageSource messageSource) {
        super(messageSource);
    }

    String getObjectTitle(String objectId, Locale locale, Object... args) {
        return messageSource.getMessage(objectId + ".title", args, locale);
    }

    String getObjectDescription(String objectId, Locale locale, Object... args) {
        return messageSource.getMessage(objectId + ".description", args, locale);
    }

    String getActionTitle(String actionId, Locale locale, Object... args) {
        return messageSource.getMessage(actionId + ".title", args, locale);
    }

    String getActionDescription(String actionId, Locale locale, Object... args) {
        return messageSource.getMessage(actionId + ".description", args, locale);
    }

    String getActionUserInputLabel(String actionId, String userInputKey, Locale locale, Object... args) {
        return messageSource.getMessage(String.format("%s.%s.label", actionId, userInputKey), args, locale);
    }
}
