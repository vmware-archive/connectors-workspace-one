/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.OpenInLink;
import com.vmware.ws1connectors.workday.models.BusinessTitleChangeTask;
import com.vmware.ws1connectors.workday.models.RequestInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.NotNull;
import java.time.OffsetTime;
import java.util.Locale;

import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotBlank;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static com.vmware.ws1connectors.workday.utils.CardConstants.BUSINESS_TITLE_CHANGE_CARD_HEADER_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.CURRENT_BUSINESS_TITLE;
import static com.vmware.ws1connectors.workday.utils.CardConstants.DUE;
import static com.vmware.ws1connectors.workday.utils.CardConstants.EFFECTIVE;
import static com.vmware.ws1connectors.workday.utils.CardConstants.INITIATED;
import static com.vmware.ws1connectors.workday.utils.CardConstants.INITIATOR;
import static com.vmware.ws1connectors.workday.utils.CardConstants.OPEN_IN_LINK_TEXT;
import static com.vmware.ws1connectors.workday.utils.CardConstants.PROPOSED_BUSINESS_TITLE;
import static com.vmware.ws1connectors.workday.utils.CardConstants.SUBJECT;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.LOCALE;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.ROUTING_PREFIX;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.TENANT_NAME;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.TENANT_URL;

@Component
@Slf4j
public class BusinessTitleChangeCardBuilder extends BaseCardBuilder
        implements NotificationCardBuilder<BusinessTitleChangeTask> {

    @Override public Card createCard(@NotNull BusinessTitleChangeTask businessTitleChangeTask,
                                     @NotNull final RequestInfo requestInfo) {
        checkArgumentNotBlank(requestInfo.getRoutingPrefix(), ROUTING_PREFIX);
        checkArgumentNotBlank(requestInfo.getTenantName(), TENANT_NAME);
        checkArgumentNotBlank(requestInfo.getTenantUrl(), TENANT_URL);
        checkArgumentNotNull(requestInfo.getLocale(), LOCALE);

        final String cardHeader = businessTitleChangeTask.getDescriptor();
        final String inboxTaskId = businessTitleChangeTask.getInboxTaskId();
        LOGGER.debug("Building card with routingPrefix={},  businessTitleChangeTaskId={}, inboxTaskId={}, cardHeader={}",
                requestInfo.getRoutingPrefix(), businessTitleChangeTask.getId(), inboxTaskId, cardHeader);
        Card.Builder builder = new Card.Builder()
                .setHeader(createCardHeader(requestInfo.getLocale(), cardHeader, BUSINESS_TITLE_CHANGE_CARD_HEADER_KEY))
                .setBody(createCardBody(businessTitleChangeTask, requestInfo.getLocale()))
                .setBackendId(inboxTaskId)
                .addLinks(OpenInLink.builder()
                        .href(UriComponentsBuilder.fromUriString(requestInfo.getTenantUrl()).build().toUri())
                        .text(cardTextAccessor.getMessage(OPEN_IN_LINK_TEXT, requestInfo.getLocale()))
                        .build())
                .addAction(createApproveCardAction(requestInfo, inboxTaskId))
                .addAction(createDeclineCardAction(requestInfo, inboxTaskId))
                .setImageUrl(getImageUrl());
        if (businessTitleChangeTask.getDue() != null) {
            builder.setDueDate(businessTitleChangeTask.getDue().atTime(OffsetTime.MIN));
        }
        return builder.build();
    }

    private CardBody createCardBody(final BusinessTitleChangeTask businessTitleChangeTask, final Locale locale) {
        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .addField(createGeneralCardBodyField(locale, CURRENT_BUSINESS_TITLE, businessTitleChangeTask.getCurrentBusinessTitle()))
                .addField(createGeneralCardBodyField(locale, PROPOSED_BUSINESS_TITLE, businessTitleChangeTask.getProposedBusinessTitle()))
                .addField(createGeneralCardBodyField(locale, SUBJECT, businessTitleChangeTask.getSubject().getDescriptor()))
                .addField(createGeneralCardBodyField(locale, EFFECTIVE, convertToString(businessTitleChangeTask.getEffective(), locale)))
                .addField(createGeneralCardBodyField(locale, DUE, convertToString(businessTitleChangeTask.getDue(), locale)))
                .addField(createGeneralCardBodyField(locale, INITIATED, convertToString(businessTitleChangeTask.getInitiated().toLocalDate(), locale)))
                .addField(createGeneralCardBodyField(locale, INITIATOR, businessTitleChangeTask.getInitiator().getDescriptor()));
        return cardBodyBuilder.build();
    }
}

