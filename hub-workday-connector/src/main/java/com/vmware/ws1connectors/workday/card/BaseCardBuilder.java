/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.connectors.common.payloads.response.CardAction;
import com.vmware.connectors.common.payloads.response.CardActionInputField;
import com.vmware.connectors.common.payloads.response.CardActionKey;
import com.vmware.connectors.common.payloads.response.CardBodyField;
import com.vmware.connectors.common.payloads.response.CardBodyFieldItem;
import com.vmware.connectors.common.payloads.response.CardBodyFieldType;
import com.vmware.connectors.common.payloads.response.CardHeader;
import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.workday.models.RequestInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static com.vmware.connectors.common.utils.CommonUtils.APPROVAL_ACTIONS;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.APPROVE_ACTION;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.DECLINE_ACTION;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIMEOFF_TASKS_API;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.URL_PATH_SEPARATOR;
import static com.vmware.ws1connectors.workday.utils.CardConstants.CARD_ACTION_INPUT_TEXTAREA;
import static com.vmware.ws1connectors.workday.utils.CardConstants.CARD_ACTION_OPEN_IN_LABEL_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.COMMENT_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.DATE_FORMAT_MMMM_DD_YYYY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.NO_HEADER_LINKS;
import static com.vmware.ws1connectors.workday.utils.CardConstants.NO_SUBTITLES;
import static com.vmware.ws1connectors.workday.utils.CardConstants.PRIMARY_ACTION;
import static com.vmware.ws1connectors.workday.utils.CardConstants.REASON_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.REASON_MIN_LENGTH;
import static com.vmware.ws1connectors.workday.utils.CardConstants.SECONDARY_ACTION;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TENANT_NAME;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_APPROVE_BUTTON_LABEL_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_APPROVE_COMMENT_LABEL_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_DECLINE_BUTTON_LABEL_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_DECLINE_REASON_LABEL_KEY;
import static org.springframework.http.HttpMethod.GET;

@Slf4j
public class BaseCardBuilder {

    @Autowired protected ConnectorTextAccessor cardTextAccessor;
    @Value("${connector.default.image}")
    private String connectorDefaultImageUrl;
    protected static final int ZERO = 0;

    protected CardHeader createCardHeader(final Locale locale, final String taskName, final String cardHeaderKey) {
        final String cardHeader = cardTextAccessor.getMessage(cardHeaderKey, locale, taskName);
        return new CardHeader(cardHeader, NO_SUBTITLES, NO_HEADER_LINKS);
    }

    protected String convertToString(final LocalDate date, final Locale locale) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay()
                .format(DateTimeFormatter.ofPattern(DATE_FORMAT_MMMM_DD_YYYY, locale));
    }

    protected CardBodyFieldItem createGeneralCardBodyFieldItem(final String titleKey, final String description, final Locale locale) {
        return new CardBodyFieldItem.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(cardTextAccessor.getMessage(titleKey, locale))
                .setDescription(description)
                .build();
    }

    protected CardAction createApproveCardAction(final RequestInfo requestInfo, final String inboxTaskId) {
        final String approveCardActionUrl = getCardActionUrl(requestInfo.getRoutingPrefix(), inboxTaskId, APPROVE_ACTION);
        return createCardAction(approveCardActionUrl, requestInfo.getLocale(), PRIMARY_ACTION, TIMEOFF_APPROVE_BUTTON_LABEL_KEY,
                COMMENT_KEY, TIMEOFF_APPROVE_COMMENT_LABEL_KEY, ZERO, requestInfo.getTenantName());
    }

    protected CardAction createDeclineCardAction(final RequestInfo requestInfo, final String inboxTaskId) {
        final String declineCardActionUrl = getCardActionUrl(requestInfo.getRoutingPrefix(), inboxTaskId, DECLINE_ACTION);
        return createCardAction(declineCardActionUrl, requestInfo.getLocale(), SECONDARY_ACTION, TIMEOFF_DECLINE_BUTTON_LABEL_KEY,
                REASON_KEY, TIMEOFF_DECLINE_REASON_LABEL_KEY, REASON_MIN_LENGTH, requestInfo.getTenantName());
    }

    private CardAction createCardAction(final String cardActionUrl, final Locale locale, final boolean primary,
                                        final String buttonLabelKey, final String textFieldId,
                                        final String textFieldLabelKey, final int minLength,
                                        final String tenantName) {

        LOGGER.info("Adding action with url: {}, primary: {}", cardActionUrl, primary);
        return new CardAction.Builder()
                .setActionKey(CardActionKey.USER_INPUT)
                .setLabel(cardTextAccessor.getActionLabel(buttonLabelKey, locale))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel(buttonLabelKey, locale))
                .setPrimary(primary)
                .setMutuallyExclusiveSetId(APPROVAL_ACTIONS)
                .setType(HttpMethod.POST)
                .setUrl(cardActionUrl)
                .addUserInputField(getCardActionInputField(locale, textFieldId, textFieldLabelKey, minLength))
                .addRequestParam(TENANT_NAME, tenantName)
                .build();
    }

    private CardActionInputField getCardActionInputField(final Locale locale, final String textFieldId,
                                                         final String textFieldLabelKey, final int minLength) {
        CardActionInputField.Builder actionInputBuilder = new CardActionInputField.Builder().setFormat(CARD_ACTION_INPUT_TEXTAREA)
                .setId(textFieldId)
                .setLabel(cardTextAccessor.getMessage(textFieldLabelKey, locale));
        actionInputBuilder.setMinLength(minLength);
        return actionInputBuilder.build();
    }

    private String getCardActionUrl(final String routingPrefix, final String inboxTaskId, final String apiPath) {
        return UriComponentsBuilder.fromHttpUrl(routingPrefix)
                .path(TIMEOFF_TASKS_API)
                .path(inboxTaskId)
                .path(URL_PATH_SEPARATOR)
                .path(apiPath)
                .build()
                .toUriString();
    }

    protected CardBodyField createGeneralCardBodyField(final Locale locale, final String titleKey, final String description) {
        if (description == null) {
            return null;
        }
        return new CardBodyField.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(cardTextAccessor.getMessage(titleKey, locale))
                .setDescription(description)
                .build();
    }

    protected CardAction openInWorkdayCardAction(String tenantUrl, Locale locale) {
        return new CardAction.Builder()
                .setActionKey(CardActionKey.OPEN_IN)
                .setLabel(cardTextAccessor.getActionLabel(CARD_ACTION_OPEN_IN_LABEL_KEY, locale))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel(CARD_ACTION_OPEN_IN_LABEL_KEY, locale))
                .setPrimary(true)
                .setType(GET)
                .setUrl(tenantUrl)
                .setAllowRepeated(true)
                .build();
    }

    protected String getImageUrl() {
        return connectorDefaultImageUrl;
    }
}

