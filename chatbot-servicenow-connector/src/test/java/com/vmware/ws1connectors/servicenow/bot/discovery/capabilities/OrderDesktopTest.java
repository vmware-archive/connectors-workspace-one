/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.bot.discovery.capabilities;

import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.servicenow.domain.BotAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OrderDesktopTest {

    private static final String ROUTING_PREFIX = "https://mf/connectors/abc123/botDiscovery/";
    private static final String BOT_ACTION_TITLE = "View List Of Desktops";
    private static final String BOT_ACTION_DESC = "You can view list of Desktops.";
    private static final String BOT_ACTION_URL = "https://mf/connectors/abc123/botDiscovery/api/v1/device_list?device_category=Desktops&limit=10&offset=0";
    private static final String ORDER_DESKTOP_ACTION = "orderDesktopAction";

    @Mock private ConnectorTextAccessor connectorTextAccessor;

    @Test public void testGetListOfDesktopsAction() {
        Locale noLocale = null;
        setMocks(noLocale);
        OrderDesktop orderDesktop = new OrderDesktop(connectorTextAccessor);
        BotAction botAction = orderDesktop.getListOfDesktopsAction(ROUTING_PREFIX, noLocale);
        assertThat(botAction.getTitle()).isEqualTo(BOT_ACTION_TITLE);
        assertThat(botAction.getDescription()).isEqualTo(BOT_ACTION_DESC);
        assertThat(botAction.getUrl().getHref()).isEqualTo(BOT_ACTION_URL);
    }

    private void setMocks(Locale locale) {
        when(connectorTextAccessor.getTitle(ORDER_DESKTOP_ACTION, locale)).thenReturn(BOT_ACTION_TITLE);
        when(connectorTextAccessor.getDescription(ORDER_DESKTOP_ACTION, locale)).thenReturn(BOT_ACTION_DESC);
    }

    @Test public void testGetListOfDesktopsActionWithRoutingPrefixNull() {
        Locale noLocale = null;
        setMocks(noLocale);
        OrderDesktop orderDesktop = new OrderDesktop(connectorTextAccessor);
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> orderDesktop.getListOfDesktopsAction(null, noLocale));
    }

}
