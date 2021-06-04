/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.OpenInLink;
import com.vmware.ws1connectors.workday.models.BusinessProcessTask;
import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.models.RequestInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.NotNull;
import java.time.OffsetTime;
import java.util.Locale;

import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotBlank;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static com.vmware.ws1connectors.workday.utils.CardConstants.APPROVAL;
import static com.vmware.ws1connectors.workday.utils.CardConstants.BUSINESS_PROCESSES_DUE;
import static com.vmware.ws1connectors.workday.utils.CardConstants.BUSINESS_PROCESSES_EFFECTIVE;
import static com.vmware.ws1connectors.workday.utils.CardConstants.BUSINESS_PROCESSES_INITIATED;
import static com.vmware.ws1connectors.workday.utils.CardConstants.BUSINESS_PROCESSES_INITIATOR;
import static com.vmware.ws1connectors.workday.utils.CardConstants.BUSINESS_PROCESSES_PROCESS;
import static com.vmware.ws1connectors.workday.utils.CardConstants.BUSINESS_PROCESSES_STATUS;
import static com.vmware.ws1connectors.workday.utils.CardConstants.BUSINESS_PROCESS_CARD_HEADER_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.OPEN_IN_LINK_TEXT;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.LOCALE;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.ROUTING_PREFIX;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.TENANT_NAME;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.TENANT_URL;

@Component
@Slf4j
public class BusinessProcessCardBuilder extends BaseCardBuilder
        implements NotificationCardBuilder<BusinessProcessTask> {

    @Override public Card createCard(@NotNull BusinessProcessTask businessProcessTask,
                                     @NotNull final RequestInfo requestInfo) {

        checkArgumentNotBlank(requestInfo.getRoutingPrefix(), ROUTING_PREFIX);
        checkArgumentNotBlank(requestInfo.getTenantName(), TENANT_NAME);
        checkArgumentNotBlank(requestInfo.getTenantUrl(), TENANT_URL);
        checkArgumentNotNull(requestInfo.getLocale(), LOCALE);

        final InboxTask inboxTask = businessProcessTask.getInboxTask();
        final String cardHeader = inboxTask.getDescriptor();
        LOGGER.debug("Building card with routingPrefix={},  businessProcessTaskId={}, inboxTaskId={}, cardHeader={}",
                requestInfo.getRoutingPrefix(), businessProcessTask.getId(), inboxTask.getId(), cardHeader);
        Card.Builder builder = new Card.Builder()
                .setHeader(createCardHeader(requestInfo.getLocale(), cardHeader, BUSINESS_PROCESS_CARD_HEADER_KEY))
                .setBody(createCardBody(businessProcessTask, inboxTask, requestInfo, requestInfo.getLocale()))
                .setBackendId(inboxTask.getId())
                .setImageUrl(getImageUrl());
        if (APPROVAL.equalsIgnoreCase(inboxTask.getStepType().getDescriptor())) {
            builder.addLinks(OpenInLink.builder()
                        .href(UriComponentsBuilder.fromUriString(requestInfo.getTenantUrl()).build().toUri())
                        .text(cardTextAccessor.getMessage(OPEN_IN_LINK_TEXT, requestInfo.getLocale()))
                        .build())
                    .addAction(createApproveCardAction(requestInfo, inboxTask.getId()))
                    .addAction(createDeclineCardAction(requestInfo, inboxTask.getId()));
        } else {
            builder.addAction(openInWorkdayCardAction(requestInfo.getTenantUrl(), requestInfo.getLocale()));
        }
        if (businessProcessTask.getDue() != null) {
            builder.setDueDate(businessProcessTask.getDue().atTime(OffsetTime.MIN));
        }
        return builder.build();
    }

    private CardBody createCardBody(final BusinessProcessTask businessProcessTask, InboxTask inboxTask, RequestInfo requestInfo, final Locale locale) {
        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .addField(createGeneralCardBodyField(locale, BUSINESS_PROCESSES_STATUS, inboxTask.getStatus().getDescriptor()))
                .addField(createGeneralCardBodyField(locale, BUSINESS_PROCESSES_EFFECTIVE, convertToString(businessProcessTask.getEffective(), locale)))
                .addField(createGeneralCardBodyField(locale, BUSINESS_PROCESSES_DUE, convertToString(businessProcessTask.getDue(), locale)))
                .addField(createGeneralCardBodyField(locale, BUSINESS_PROCESSES_INITIATED, convertToString(businessProcessTask.getInitiated().toLocalDate(), locale)))
                .addField(createGeneralCardBodyField(locale, BUSINESS_PROCESSES_INITIATOR, businessProcessTask.getInitiator().getDescriptor()));
        if (!requestInfo.isPreHire()) {
            cardBodyBuilder.addField(createGeneralCardBodyField(locale, BUSINESS_PROCESSES_PROCESS, businessProcessTask.getDescriptor()));
        }
        return cardBodyBuilder.build();
    }
}
