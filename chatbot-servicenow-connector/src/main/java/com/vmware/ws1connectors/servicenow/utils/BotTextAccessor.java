/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.connectors.common.utils.TextAccessorBase;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class BotTextAccessor extends TextAccessorBase {

    @Autowired public BotTextAccessor(MessageSource messageSource) {
        super(messageSource);
    }

    public String getObjectTitle(String objectId, Locale locale, Object... args) {
        return messageSource.getMessage(objectId + ServiceNowConstants.TITLE, args, locale);
    }

    public String getObjectDescription(String objectId, Locale locale, Object... args) {
        return messageSource.getMessage(objectId + ServiceNowConstants.DESCRIPTION, args, locale);
    }

    public String getActionTitle(String actionId, Locale locale, Object... args) {
        return messageSource.getMessage(actionId + ServiceNowConstants.TITLE, args, locale);
    }

    public String getActionDescription(String actionId, Locale locale, Object... args) {
        return messageSource.getMessage(actionId + ServiceNowConstants.DESCRIPTION, args, locale);
    }

    public String getActionUserInputLabel(String actionId, String userInputKey, Locale locale, Object... args) {
        return messageSource.getMessage(String.format("%s.%s" + ServiceNowConstants.LABEL, actionId, userInputKey), args, locale);
    }
}
