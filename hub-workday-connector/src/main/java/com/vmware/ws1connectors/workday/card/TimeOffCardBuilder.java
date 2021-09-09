/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.google.common.collect.Lists;
import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.CardBodyField;
import com.vmware.connectors.common.payloads.response.CardBodyFieldItem;
import com.vmware.connectors.common.payloads.response.CardBodyFieldType;
import com.vmware.connectors.common.payloads.response.OpenInLink;
import com.vmware.ws1connectors.workday.models.RequestInfo;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffItem;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotBlank;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static com.vmware.ws1connectors.workday.utils.CardConstants.CARD_HEADER_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.OPEN_IN_LINK_TEXT;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_ASSIGNED_ON_KEY;
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
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.ROUTING_PREFIX;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.TENANT_NAME;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.TENANT_URL;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class TimeOffCardBuilder extends BaseCardBuilder implements NotificationCardBuilder<TimeOffTask> {

    private static final int ONE_DAY_PTO = 1;

    @Override public Card createCard(@NotNull final TimeOffTask timeOffTask, @NotNull final RequestInfo requestInfo) {
        checkArgumentNotBlank(requestInfo.getRoutingPrefix(), ROUTING_PREFIX);
        checkArgumentNotBlank(requestInfo.getTenantName(), TENANT_NAME);
        checkArgumentNotBlank(requestInfo.getTenantUrl(), TENANT_URL);
        checkArgumentNotNull(requestInfo.getLocale(), LOCALE);

        final String inboxTaskName = timeOffTask.getTimeOffTaskDescriptor().getDescriptor();
        final String inboxTaskId = timeOffTask.getInboxTaskId();
        LOGGER.debug("Building card with routingPrefix={}, timeOffRequestId={}, inboxTaskId={}, inboxTaskName={}",
            requestInfo.getRoutingPrefix(), timeOffTask.getTimeOffTaskDescriptor().getId(), inboxTaskId, inboxTaskName);
        Card.Builder builder = new Card.Builder()
            .setHeader(createCardHeader(requestInfo.getLocale(), inboxTaskName, CARD_HEADER_KEY))
            .setBody(createCardBody(timeOffTask, requestInfo.getLocale()))
            .setBackendId(inboxTaskId)
            .addLinks(OpenInLink.builder()
                    .href(UriComponentsBuilder.fromUriString(requestInfo.getTenantUrl()).build().toUri())
                    .text(cardTextAccessor.getMessage(OPEN_IN_LINK_TEXT, requestInfo.getLocale()))
                    .build())
            .addAction(createApproveCardAction(requestInfo, inboxTaskId))
            .addAction(createDeclineCardAction(requestInfo, inboxTaskId));

        // Set image url.
        builder.setImageUrl(getImageUrl());
        return builder.build();
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
}
