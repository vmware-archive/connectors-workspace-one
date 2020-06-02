/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.connectors.common.payloads.response.CardBodyFieldItem;
import com.vmware.connectors.common.payloads.response.CardBodyFieldType;
import com.vmware.connectors.common.payloads.response.CardHeader;
import com.vmware.connectors.common.utils.CardTextAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Locale;

import static com.vmware.ws1connectors.workday.utils.CardConstants.CARD_HEADER_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.NO_HEADER_LINKS;
import static com.vmware.ws1connectors.workday.utils.CardConstants.NO_SUBTITLES;

@Slf4j
public class BaseCardBuilder {

    @Autowired protected CardTextAccessor cardTextAccessor;
    @Value("${connector.default.image}")
    private String connectorDefaultImageUrl;

    protected CardHeader createCardHeader(final Locale locale, final String taskName) {
        final String cardHeader = cardTextAccessor.getMessage(CARD_HEADER_KEY, locale, taskName);
        return new CardHeader(cardHeader, NO_SUBTITLES, NO_HEADER_LINKS);
    }

    protected CardBodyFieldItem createGeneralCardBodyFieldItem(final String titleKey, final String description, final Locale locale) {
        return new CardBodyFieldItem.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(cardTextAccessor.getMessage(titleKey, locale))
                .setDescription(description)
                .build();
    }

    protected String getImageUrl() {
        return connectorDefaultImageUrl;
    }
}

