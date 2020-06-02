/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.ws1connectors.servicenow.constants.ServiceNowCategory;
import com.vmware.ws1connectors.servicenow.domain.BotAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import java.util.Locale;
import java.util.stream.Stream;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SERVICE_NOW_CONNECTOR_CONTEXT_PATH;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.URL_PATH_SEPERATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BotActionBuilderTest {

    private static final String ROUTING_PREFIX = "https://mf/connectors/abc123/botDiscovery/";
    private static final String BOT_ACTION_TITLE = "Add Another";
    private static final String BOT_ACTION_DESC = "Add another item to cart";
    private static final String BOT_ACTION_URL = "https://mf/connectors/abc123/botDiscovery/servicenow-connector/api/v1/deviceCategoryList";
    private static final String ADD_ANOTHER_ITEM_TITLE = "add.another.title";
    private static final String ADD_ANOTHER_ITEM_DESC = "add.another.description";
    private static final String ORDER_DESKTOP_TITLE = "View List Of Desktops";
    private static final String ORDER_DESKTOP_DESC = "You can view list of Desktops";
    private static final String ORDER_DESKTOP_ACTION_URL = "https://mf/connectors/abc123/botDiscovery/servicenow-connector/api/v1/device_list?device_category=Desktops&limit=10&offset=0";
    private static final String NO_ROUTING_PREFIX = null;
    private static final String EMPTY_ROUTING_PREFIX = "";
    private static final String ORDER_DESKTOP_ACTION_DESC = "orderDesktopAction.description";
    private static final String ORDER_DESKTOP_ACTION_TITLE = "orderDesktopAction.title";

    @InjectMocks private BotActionBuilder botActionBuilder;
    @Mock private BotTextAccessor botTextAccessor;
    @Mock private ExchangeFunction mockExchangeFunc;
    @Mock ServerProperties mockServerProperties;

    @Test public void testGetAddAnotherItemCartAction() {
        Locale noLocale = null;
        setMocks(noLocale);
        BotAction botAction = botActionBuilder.getAddAnotherItemCartAction(ROUTING_PREFIX, noLocale);
        assertThat(botAction.getTitle()).isEqualTo(BOT_ACTION_TITLE);
        assertThat(botAction.getDescription()).isEqualTo(BOT_ACTION_DESC);
        assertThat(botAction.getUrl().getHref()).isEqualTo(BOT_ACTION_URL);
    }

    private void setMocks(Locale locale) {
        when(botTextAccessor.getMessage(ADD_ANOTHER_ITEM_TITLE, locale)).thenReturn(BOT_ACTION_TITLE);
        when(botTextAccessor.getMessage(ADD_ANOTHER_ITEM_DESC, locale)).thenReturn(BOT_ACTION_DESC);
    }

    @Test public void testGetAddAnotherItemCartActionThrowsExceptionWhenTitleIsNull() {
        Locale noLocale = null;
        setMocksWithTitleAsNull(noLocale);
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> botActionBuilder.getAddAnotherItemCartAction(ROUTING_PREFIX, noLocale));
    }

    @Test public void testGetAddAnotherItemCartActionThrowsExceptionWhenDescriptionIsNull() {
        Locale noLocale = null;
        setMocksWithDescriptionAsNull(noLocale);
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> botActionBuilder.getAddAnotherItemCartAction(ROUTING_PREFIX, noLocale));
    }

    private void setMocksWithDescriptionAsNull(Locale locale) {
        when(botTextAccessor.getMessage(ADD_ANOTHER_ITEM_TITLE, locale)).thenReturn(BOT_ACTION_TITLE);
        when(botTextAccessor.getMessage(ADD_ANOTHER_ITEM_DESC, locale)).thenThrow(NullPointerException.class);
    }

    private void setMocksWithTitleAsNull(Locale locale) {
        when(botTextAccessor.getMessage(ADD_ANOTHER_ITEM_TITLE, locale)).thenThrow(NullPointerException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testGetAddAnotherItemCartActionThrowsExceptionWithInvalidInputs(final String routingPrefix) {
        Locale noLocale = null;
        setMocks(noLocale);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> botActionBuilder.getAddAnotherItemCartAction(routingPrefix, noLocale));
        verify(mockExchangeFunc, never()).exchange(any(ClientRequest.class));
    }

    @Test public void testBuildBotActionForDeviceCategory() {
        Locale noLocale = null;
        mockContextPathForServerProperties();
        setMocksForDeviceCategory(noLocale);
        BotAction botAction = botActionBuilder.buildBotActionForDeviceCategory(ServiceNowCategory.DESKTOP, ROUTING_PREFIX, noLocale);
        assertThat(botAction.getTitle()).isEqualTo(ORDER_DESKTOP_TITLE);
        assertThat(botAction.getDescription()).isEqualTo(ORDER_DESKTOP_DESC);
        assertThat(botAction.getUrl().getHref()).isEqualTo(ORDER_DESKTOP_ACTION_URL);
    }

    private void setMocksForDeviceCategory(Locale locale) {
        when(botTextAccessor.getMessage(ORDER_DESKTOP_ACTION_TITLE, locale)).thenReturn(ORDER_DESKTOP_TITLE);
        when(botTextAccessor.getMessage(ORDER_DESKTOP_ACTION_DESC, locale)).thenReturn(ORDER_DESKTOP_DESC);
    }

    private void mockContextPathForServerProperties() {
        final ServerProperties.Servlet mockServlet = mock(ServerProperties.Servlet.class);
        when(mockServerProperties.getServlet()).thenReturn(mockServlet);
        when(mockServlet.getContextPath()).thenReturn(URL_PATH_SEPERATOR + SERVICE_NOW_CONNECTOR_CONTEXT_PATH);
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForBuildBotActionForDeviceCategory")
    public void testBuildBotActionForDeviceCategoryWithInvalidInputs(final ServiceNowCategory category, final String routingPrefix) {
        Locale noLocale = null;
        mockContextPathForServerProperties();
        setMocksForDeviceCategory(noLocale);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> botActionBuilder.buildBotActionForDeviceCategory(category, routingPrefix, noLocale));
        verify(mockExchangeFunc, never()).exchange(any(ClientRequest.class));
    }

    private static Stream<Arguments> invalidInputsForBuildBotActionForDeviceCategory() {
        return new ArgumentsStreamBuilder()
                .add(null, ROUTING_PREFIX)
                .add(ServiceNowCategory.DESKTOP, NO_ROUTING_PREFIX)
                .add(ServiceNowCategory.DESKTOP, EMPTY_ROUTING_PREFIX)
                .build();
    }
}
