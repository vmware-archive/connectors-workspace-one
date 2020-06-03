/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.google.common.collect.Lists;
import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardAction;
import com.vmware.connectors.common.payloads.response.CardActionInputField;
import com.vmware.connectors.common.payloads.response.CardActionKey;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.CardBodyField;
import com.vmware.connectors.common.payloads.response.CardBodyFieldItem;
import com.vmware.connectors.common.payloads.response.CardBodyFieldType;
import com.vmware.connectors.common.payloads.response.CardHeader;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffItem;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.vmware.connectors.common.utils.CommonUtils.APPROVAL_ACTIONS;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.APPROVE_ACTION;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.DECLINE_ACTION;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIMEOFF_TASKS_API;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.URL_PATH_SEPARATOR;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotBlank;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static com.vmware.ws1connectors.workday.utils.CardConstants.CARD_ACTION_INPUT_TEXTAREA;
import static com.vmware.ws1connectors.workday.utils.CardConstants.CARD_HEADER_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.COMMENT_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.DATE_FORMAT_MMMM_DD_YYYY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.NO_HEADER_LINKS;
import static com.vmware.ws1connectors.workday.utils.CardConstants.PRIMARY_ACTION;
import static com.vmware.ws1connectors.workday.utils.CardConstants.REASON_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.REASON_MIN_LENGTH;
import static com.vmware.ws1connectors.workday.utils.CardConstants.SECONDARY_ACTION;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_APPROVE_BUTTON_LABEL_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_APPROVE_COMMENT_LABEL_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_ASSIGNED_ON_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_DECLINE_BUTTON_LABEL_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_DECLINE_REASON_LABEL_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_END_DATE_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_ENTRY_DATE_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_ENTRY_DAY_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_ENTRY_TITLE_DAY_INDEXING_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_ENTRY_TITLE_DETAILS_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_ENTRY_TOTAL_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_ENTRY_TYPE_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_REQUESTED_BY_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_START_DATE_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_TOTAL_KEY;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.LOCALE;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.REQUEST;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.ROUTING_PREFIX;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "squid:S1075"})
public class CardBuilder {
    private static final int ZERO = 0;
    private static final int ONE_DAY_PTO = 1;
    public static final String TIME_OFF_TASK = "TimeOffTask";
    private static final List<String> NO_SUBTITLES = null;

    @Autowired private CardTextAccessor cardTextAccessor;
    @Autowired ServerProperties serverProperties;
    @Value("${connector.default.image}")
    private String connectorDefaultImageUrl;

    public Card createCard(final String routingPrefix, final Locale locale,
                           final TimeOffTask timeOffTask, final ServerHttpRequest request) {
        checkArgumentNotBlank(routingPrefix, ROUTING_PREFIX);
        checkArgumentNotNull(locale, LOCALE);
        checkArgumentNotNull(timeOffTask, TIME_OFF_TASK);
        checkArgumentNotNull(request, REQUEST);

        final String inboxTaskName = timeOffTask.getTimeOffTaskDescriptor().getDescriptor();
        final String inboxTaskId = timeOffTask.getInboxTaskId();
        LOGGER.info("Building card with routingPrefix={}, timeOffRequestId={}, inboxTaskId={}, inboxTaskName={}",
            routingPrefix, timeOffTask.getTimeOffTaskDescriptor().getId(), inboxTaskId, inboxTaskName);
        Card.Builder builder = new Card.Builder()
            .setHeader(createCardHeader(locale, inboxTaskName))
            .setBody(createCardBody(timeOffTask, locale))
            .setBackendId(inboxTaskId)
            .addAction(createApproveCardAction(routingPrefix, locale, inboxTaskId))
            .addAction(createDeclineCardAction(routingPrefix, locale, inboxTaskId));

        // Set image url.
        builder.setImageUrl(getImageUrl());
        return builder.build();
    }

    private CardHeader createCardHeader(final Locale locale, final String taskName) {
        final String cardHeader = cardTextAccessor.getMessage(CARD_HEADER_KEY, locale, taskName);
        return new CardHeader(cardHeader, NO_SUBTITLES, NO_HEADER_LINKS);
    }

    private CardBody createCardBody(final TimeOffTask timeOffApprovalTask, final Locale locale) {
        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
            .addField(createGeneralCardBodyField(locale, TIMEOFF_START_DATE_KEY, convertToString(timeOffApprovalTask.getStartDate(), locale)))
            .addField(createGeneralCardBodyField(locale, TIMEOFF_END_DATE_KEY, convertToString(timeOffApprovalTask.getEndDate(), locale)))
            .addField(createGeneralCardBodyField(locale, TIMEOFF_TOTAL_KEY, timeOffApprovalTask.getTotalTimeOffDuration()))
            .addField(createGeneralCardBodyField(locale, TIMEOFF_REQUESTED_BY_KEY, timeOffApprovalTask.getSubject()))
            .addField(createGeneralCardBodyField(locale, TIMEOFF_ASSIGNED_ON_KEY, convertToString(timeOffApprovalTask.getAssignedOn().toLocalDate(), locale)));

        if (!CollectionUtils.isEmpty(timeOffApprovalTask.getTimeOffItems())) {
            buildTimeOffApprovalItemFields(timeOffApprovalTask, locale).forEach(cardBodyBuilder::addField);
        }
        return cardBodyBuilder.build();
    }

    private String convertToString(final LocalDate date, final Locale locale) {
        return date.atStartOfDay()
            .format(DateTimeFormatter.ofPattern(DATE_FORMAT_MMMM_DD_YYYY, locale));
    }

    private List<CardBodyField> buildTimeOffApprovalItemFields(final TimeOffTask timeOffApprovalTask, final Locale locale) {
        final List<TimeOffItem> timeOffItems = timeOffApprovalTask.getTimeOffItems();
        return IntStream.range(ZERO, timeOffItems.size())
            .mapToObj(index -> {
                TimeOffItem approvalItem = timeOffApprovalTask.getTimeOffItems().get(index);
                return new CardBodyField.Builder()
                    .setType(CardBodyFieldType.SECTION)
                    .addItems(createCardBodyFieldItems(approvalItem, locale))
                    .setTitle(getTimeOffItemTitle(timeOffItems.size(), locale, index))
                    .build();
            })
            .collect(Collectors.toList());
    }

    private String getTimeOffItemTitle(final int noOfPtoDays, final Locale locale, final int index) {
        final String title;
        if (ZERO == index && ONE_DAY_PTO == noOfPtoDays) {
            title = cardTextAccessor.getMessage(TIMEOFF_ENTRY_TITLE_DETAILS_KEY, locale);
        } else {
            title = cardTextAccessor.getMessage(TIMEOFF_ENTRY_TITLE_DAY_INDEXING_KEY, locale, index + 1);
        }
        return title;
    }

    private List<CardBodyFieldItem> createCardBodyFieldItems(final TimeOffItem approvalItem, final Locale locale) {
        return getBodyFieldTitleAndDescriptionPairs(approvalItem, locale).stream()
            .filter(titleAndDescriptionPair -> isNotBlank(titleAndDescriptionPair.getRight()))
            .map(titleAndDescriptionPair -> {
                final String title = titleAndDescriptionPair.getLeft();
                final String description = titleAndDescriptionPair.getRight();
                return createGeneralCardBodyFieldItem(title, description, locale);
            })
            .collect(Collectors.toList());
    }

    private List<Pair<String, String>> getBodyFieldTitleAndDescriptionPairs(final TimeOffItem approvalItem, final Locale locale) {
        return Lists.newArrayList(Pair.of(TIMEOFF_ENTRY_DATE_KEY, convertToString(approvalItem.getDate(), locale)),
            Pair.of(TIMEOFF_ENTRY_TYPE_KEY, approvalItem.getType()),
            Pair.of(TIMEOFF_ENTRY_DAY_KEY, approvalItem.getDayOfWeek()),
            Pair.of(TIMEOFF_ENTRY_TOTAL_KEY, approvalItem.getRequestedTimeOffQuantity() + SPACE + approvalItem.getUnitOfTime()));
    }

    private CardBodyFieldItem createGeneralCardBodyFieldItem(final String titleKey, final String description, final Locale locale) {
        return new CardBodyFieldItem.Builder()
            .setType(CardBodyFieldType.GENERAL)
            .setTitle(cardTextAccessor.getMessage(titleKey, locale))
            .setDescription(description)
            .build();
    }

    private CardBodyField createGeneralCardBodyField(final Locale locale, final String titleKey, final String description) {
        return new CardBodyField.Builder()
            .setType(CardBodyFieldType.GENERAL)
            .setTitle(cardTextAccessor.getMessage(titleKey, locale))
            .setDescription(description)
            .build();
    }

    private CardAction createApproveCardAction(final String routingPrefix, final Locale locale, final String inboxTaskId) {
        final String approveCardActionUrl = getCardActionUrl(routingPrefix, inboxTaskId, APPROVE_ACTION);
        return createCardAction(approveCardActionUrl, locale, PRIMARY_ACTION, TIMEOFF_APPROVE_BUTTON_LABEL_KEY,
            COMMENT_KEY, TIMEOFF_APPROVE_COMMENT_LABEL_KEY, ZERO);
    }

    private CardAction createDeclineCardAction(final String routingPrefix, final Locale locale, final String inboxTaskId) {
        final String declineCardActionUrl = getCardActionUrl(routingPrefix, inboxTaskId, DECLINE_ACTION);
        return createCardAction(declineCardActionUrl, locale, SECONDARY_ACTION, TIMEOFF_DECLINE_BUTTON_LABEL_KEY,
            REASON_KEY, TIMEOFF_DECLINE_REASON_LABEL_KEY, REASON_MIN_LENGTH);
    }

    private CardAction createCardAction(final String cardActionUrl, final Locale locale, final boolean primary,
                                        final String buttonLabelKey, final String textFieldId,
                                        final String textFieldLabelKey, final int minLength) {

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
        final String urlRoutingPrefix = StringUtils.removeEnd(routingPrefix, URL_PATH_SEPARATOR);
        return UriComponentsBuilder.fromHttpUrl(urlRoutingPrefix)
            .path(StringUtils.appendIfMissing(serverProperties.getServlet().getContextPath(), URL_PATH_SEPARATOR))
            .path(TIMEOFF_TASKS_API)
            .path(inboxTaskId)
            .path(URL_PATH_SEPARATOR)
            .path(apiPath)
            .build()
            .toUriString();
    }

    protected String getImageUrl() {
        return connectorDefaultImageUrl;
    }

}
