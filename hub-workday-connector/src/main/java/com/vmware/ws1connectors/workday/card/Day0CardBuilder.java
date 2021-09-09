/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardAction;
import com.vmware.connectors.common.payloads.response.CardActionKey;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.CardBodyField;
import com.vmware.connectors.common.payloads.response.CardBodyFieldItem;
import com.vmware.connectors.common.payloads.response.CardBodyFieldType;
import com.vmware.ws1connectors.workday.models.InboxTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotBlank;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static com.vmware.ws1connectors.workday.utils.CardConstants.CARD_HEADER_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.ONBOARDING_SETUP;
import static com.vmware.ws1connectors.workday.utils.CardConstants.OPEN_IN_WORKDAY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TASK_DUE;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TASK_NAME;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TASK_TITLE;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.BASE_URL_HEADER;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.LOCALE;

@Component
@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class Day0CardBuilder extends BaseCardBuilder {

    private static final String INBOX_TASK = "inboxTask";
    private static final String CARD_BODY_DESCRIPTION_WITH_TWO_TASK = "You have %s and %s tasks to complete in Workday!";
    private static final String CARD_BODY_DESCRIPTION_WITH_SINGLE_TASK = "You have %s task to complete in Workday!";
    private static final String CARD_BODY_DESCRIPTION_MANY_TASKS = "You have %s and other %d pending tasks to complete in Workday!";
    private static final int TWO = 2;
    private static final int ONE = 1;
    private static final int ZERO = 0;

    public Card createCard(final String baseUrl, final Locale locale, final List<InboxTask> inboxTasks) {
        checkArgumentNotBlank(baseUrl, BASE_URL_HEADER);
        checkArgumentNotNull(locale, LOCALE);
        checkArgumentNotNull(inboxTasks, INBOX_TASK);
        checkInboxTaskNotEmpty(inboxTasks);

        Card.Builder builder = new Card.Builder()
                .setHeader(createCardHeader(locale, ONBOARDING_SETUP, CARD_HEADER_KEY))
                .setBody(createCardBody(inboxTasks, locale))
                .addAction(createCardAction(baseUrl));
        builder.setImageUrl(getImageUrl());
        return builder.build();
    }

    private void checkInboxTaskNotEmpty(List<InboxTask> inboxTasks) {
        if (inboxTasks.isEmpty()) {
            throw new IllegalArgumentException("Inbox Task Empty");
        }
    }

    private CardBody createCardBody(List<InboxTask> inboxTasks, Locale locale) {
        CardBody.Builder cardBodyBuilder = new CardBody.Builder();
        String taskName = inboxTasks.get(0).getOverallProcess().getDescriptor();
        if (inboxTasks.size() == TWO) {
            String taskName2 = inboxTasks.get(1).getOverallProcess().getDescriptor();
            cardBodyBuilder.setDescription(String.format(CARD_BODY_DESCRIPTION_WITH_TWO_TASK, taskName, taskName2));
        } else if (inboxTasks.size() == ONE) {
            cardBodyBuilder.setDescription(String.format(CARD_BODY_DESCRIPTION_WITH_SINGLE_TASK, taskName));
        } else {
            cardBodyBuilder.setDescription(String.format(CARD_BODY_DESCRIPTION_MANY_TASKS, taskName, inboxTasks.size() - 1));
        }
        buildCardBodyField(inboxTasks, locale, cardBodyBuilder);
        return cardBodyBuilder.build();
    }

    private void buildCardBodyField(List<InboxTask> inboxTasks, Locale locale, CardBody.Builder cardBodyBuilder) {
        IntStream.range(ZERO, inboxTasks.size())
                .mapToObj(index -> {
                    InboxTask inboxTask = inboxTasks.get(index);
                    String title = cardTextAccessor.getMessage(TASK_TITLE, locale, index + 1);
                    List<CardBodyFieldItem> cardBodyFieldItems = List.of(
                            createGeneralCardBodyFieldItem(TASK_NAME, inboxTask.getOverallProcess().getDescriptor(), locale),
                            createGeneralCardBodyFieldItem(TASK_DUE, convertToString(inboxTask.getDue(), locale), locale));
                    return new CardBodyField.Builder()
                            .setType(CardBodyFieldType.SECTION)
                            .addItems(cardBodyFieldItems)
                            .setTitle(title)
                            .build();

                }).forEach(cardBodyBuilder::addField);
    }

    private CardAction createCardAction(final String baseUrl) {
        return new CardAction.Builder()
                .setActionKey(CardActionKey.OPEN_IN)
                .setLabel(OPEN_IN_WORKDAY)
                .setPrimary(true)
                .setCompletedLabel(OPEN_IN_WORKDAY)
                .setType(HttpMethod.GET)
                .setUrl(baseUrl)
                .setRemoveCardOnCompletion(false)
                .setAllowRepeated(true)
                .build();
    }
}

