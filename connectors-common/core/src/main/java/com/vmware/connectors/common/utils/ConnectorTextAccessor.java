/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */


package com.vmware.connectors.common.utils;

import org.springframework.context.MessageSource;

import java.util.Locale;

public class ConnectorTextAccessor extends TextAccessorBase {

    private static final String TITLE = ".title";
    private static final String DESCRIPTION = ".description";
    private static final String LABEL = ".label";
    private static final String COMPLETED_LABEL = ".completedLabel";
    private static final String HEADER = "header";
    private static final String BODY = "body";

    public ConnectorTextAccessor(MessageSource messageSource) {
        super(messageSource);
    }

    /*
     *Return text message from messageSource for element name appended by title
     * */
    public String getTitle(String elementId, Locale locale, Object... args) {
        return messageSource.getMessage(elementId + TITLE, args, locale);
    }

    /*
     *Return text message from messageSource for element name appended by description
     * */
    public String getDescription(String elementId, Locale locale, Object... args) {
        return messageSource.getMessage(elementId + DESCRIPTION, args, locale);
    }

    /*
     *Return text message from messageSource for ActionUserInputLabel name appended by label
     * */
    public String getActionUserInputLabel(String actionId, String userInputKey, Locale locale, Object... args) {
        return messageSource.getMessage(String.format("%s.%s" + LABEL, actionId, userInputKey), args, locale);
    }

    /*
     *Return text message from messageSource for ActionLabel name appended by label
     * */
    public String getActionLabel(String actionId, Locale locale, Object... args) {
        return messageSource.getMessage(actionId + LABEL, args, locale);
    }

    /*
     *Return text message from messageSource for ActionCompletedLabel name appended by completedLabel
     * */
    public String getActionCompletedLabel(String actionId, Locale locale, Object... args) {
        return messageSource.getMessage(actionId + COMPLETED_LABEL, args, locale);
    }

    /*
     *Return text message from messageSource for header
     * */
    public String getHeader(Locale locale, Object... args) {
        return messageSource.getMessage(HEADER, args, locale);
    }

    /*
     *Return text message from messageSource for body
     * */
    public String getBody(Locale locale, Object... args) {
        return messageSource.getMessage(BODY, args, locale);
    }

}
