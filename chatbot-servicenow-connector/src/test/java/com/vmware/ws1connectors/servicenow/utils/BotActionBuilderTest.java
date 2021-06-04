/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.connectors.common.utils.ConnectorTextAccessor;
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
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import java.util.stream.Stream;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.TYPE_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BotActionBuilderTest {

    private static final String ROUTING_PREFIX = "https://mf/connectors/abc123/botDiscovery/";
    private static final String BOT_ACTION_TITLE = "Add Another";
    private static final String BOT_ACTION_DESC = "Add another item to cart";
    private static final String BOT_ACTION_URL = "https://mf/connectors/abc123/botDiscovery/api/v1/deviceCategoryList";
    private static final String ADD_ANOTHER = "add.another";
    private static final String ORDER_DESKTOP_TITLE = "View List Of Desktops";
    private static final String ORDER_DESKTOP_DESC = "You can view list of Desktops";
    private static final String ORDER_DESKTOP_ACTION_URL = "https://mf/connectors/abc123/botDiscovery/api/v1/device_list?device_category=Desktops&limit=10&offset=0";
    private static final String NO_ROUTING_PREFIX = null;
    private static final String EMPTY_ROUTING_PREFIX = "";
    private static final String ORDER_DESKTOP_ACTION = "orderDesktopAction";
    private static final String OPTION_YES_TITLE = "option.yes";
    private static final String OPTION_YES_DESC = "option.yes";
    private static final String OPTION_YES = "Yes";
    private static final String OPTION_NO_TITLE = "option.no";
    private static final String OPTION_NO_DESC = "option.no";
    private static final String OPTION_NO = "No";
    private static final String CREATE_TASK_CONFIRMATION_URL = "https://mf/connectors/abc123/botDiscovery/api/v1/task/create";
    private static final String CREATE_TASK_URL = "api/v1/task/create";
    private static final String CHECKOUT_URL = "api/v1/checkout";
    private static final String SHORT_DESCRIPTION = "shortDescription";
    private static final String CHECK_OUT_CONFIRMATION_URL = "https://mf/connectors/abc123/botDiscovery/api/v1/checkout";
    private static final String DECLINE_WORKFLOW_URL = "https://mf/connectors/abc123/botDiscovery/api/v1/operation/cancel";

    @InjectMocks private BotActionBuilder botActionBuilder;
    @Mock private ConnectorTextAccessor connectorTextAccessor;
    @Mock private ExchangeFunction mockExchangeFunc;

    @Test public void testGetAddAnotherItemCartAction() {
        setMocks();
        BotAction botAction = botActionBuilder.getAddAnotherItemCartAction(ROUTING_PREFIX, null);
        assertThat(botAction.getTitle()).isEqualTo(BOT_ACTION_TITLE);
        assertThat(botAction.getDescription()).isEqualTo(BOT_ACTION_DESC);
        assertThat(botAction.getUrl().getHref()).isEqualTo(BOT_ACTION_URL);
    }

    private void setMocks() {
        when(connectorTextAccessor.getTitle(ADD_ANOTHER, null)).thenReturn(BOT_ACTION_TITLE);
        when(connectorTextAccessor.getDescription(ADD_ANOTHER, null)).thenReturn(BOT_ACTION_DESC);
    }

    @Test public void testGetAddAnotherItemCartActionThrowsExceptionWhenTitleIsNull() {
        setMocksWithTitleAsNull();
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> botActionBuilder.getAddAnotherItemCartAction(ROUTING_PREFIX, null));
    }

    @Test public void testGetAddAnotherItemCartActionThrowsExceptionWhenDescriptionIsNull() {
        setMocksWithDescriptionAsNull();
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> botActionBuilder.getAddAnotherItemCartAction(ROUTING_PREFIX, null));
    }

    private void setMocksWithDescriptionAsNull() {
        when(connectorTextAccessor.getTitle(ADD_ANOTHER, null)).thenReturn(BOT_ACTION_TITLE);
        when(connectorTextAccessor.getDescription(ADD_ANOTHER, null)).thenThrow(NullPointerException.class);
    }

    private void setMocksWithTitleAsNull() {
        when(connectorTextAccessor.getTitle(ADD_ANOTHER, null)).thenThrow(NullPointerException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testGetAddAnotherItemCartActionThrowsExceptionWithInvalidInputs(final String routingPrefix) {
        setMocks();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> botActionBuilder.getAddAnotherItemCartAction(routingPrefix, null));
        verify(mockExchangeFunc, never()).exchange(any(ClientRequest.class));
    }

    @Test public void testBuildBotActionForDeviceCategory() {
        setMocksForDeviceCategory();
        BotAction botAction = botActionBuilder.buildBotActionForDeviceCategory(ServiceNowCategory.DESKTOP, ROUTING_PREFIX, null);
        assertThat(botAction.getTitle()).isEqualTo(ORDER_DESKTOP_TITLE);
        assertThat(botAction.getDescription()).isEqualTo(ORDER_DESKTOP_DESC);
        assertThat(botAction.getUrl().getHref()).isEqualTo(ORDER_DESKTOP_ACTION_URL);
    }

    private void setMocksForDeviceCategory() {
        when(connectorTextAccessor.getTitle(ORDER_DESKTOP_ACTION, null)).thenReturn(ORDER_DESKTOP_TITLE);
        when(connectorTextAccessor.getDescription(ORDER_DESKTOP_ACTION, null)).thenReturn(ORDER_DESKTOP_DESC);
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForBuildBotActionForDeviceCategory")
    public void testBuildBotActionForDeviceCategoryWithInvalidInputs(final ServiceNowCategory category, final String routingPrefix) {
        setMocksForDeviceCategory();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> botActionBuilder.buildBotActionForDeviceCategory(category, routingPrefix, null));
        verify(mockExchangeFunc, never()).exchange(any(ClientRequest.class));
    }

    private static Stream<Arguments> invalidInputsForBuildBotActionForDeviceCategory() {
        return new ArgumentsStreamBuilder()
                .add(null, ROUTING_PREFIX)
                .add(ServiceNowCategory.DESKTOP, NO_ROUTING_PREFIX)
                .add(ServiceNowCategory.DESKTOP, EMPTY_ROUTING_PREFIX)
                .build();
    }

    @Test public void testConfirmTaskCreate() {
        when(connectorTextAccessor.getTitle(OPTION_YES_TITLE, null)).thenReturn(OPTION_YES);
        when(connectorTextAccessor.getDescription(OPTION_YES_DESC, null)).thenReturn(OPTION_YES);

        final String shortDescription = "short test description" + (Math.random() * 1000);
        final String taskType = "testType";

        BotAction botAction = botActionBuilder.confirmTaskCreate(shortDescription, ROUTING_PREFIX, null, CREATE_TASK_URL, taskType);
        assertThat(botAction.getTitle()).isEqualTo(OPTION_YES);
        assertThat(botAction.getDescription()).isEqualTo(OPTION_YES);
        assertThat(botAction.getUrl().getHref()).isEqualTo(CREATE_TASK_CONFIRMATION_URL);
        assertThat(botAction.getHeaders().get(HttpHeaders.CONTENT_TYPE)).isEqualTo(APPLICATION_FORM_URLENCODED_VALUE);
        assertThat(botAction.getPayload().get(TYPE_KEY)).isEqualTo(taskType);
        assertThat(botAction.getPayload().get(SHORT_DESCRIPTION)).isEqualTo(shortDescription);
    }

    @Test public void testConfirmCartCheckout() {
        when(connectorTextAccessor.getTitle(OPTION_YES_TITLE, null)).thenReturn(OPTION_YES);
        when(connectorTextAccessor.getDescription(OPTION_YES_DESC, null)).thenReturn(OPTION_YES);

        BotAction botAction = botActionBuilder.confirmCartCheckout(ROUTING_PREFIX, null, CHECKOUT_URL);
        assertThat(botAction.getTitle()).isEqualTo(OPTION_YES);
        assertThat(botAction.getDescription()).isEqualTo(OPTION_YES);
        assertThat(botAction.getUrl().getHref()).isEqualTo(CHECK_OUT_CONFIRMATION_URL);
    }

    @Test public void testDeclineWorkflow() {
        when(connectorTextAccessor.getTitle(OPTION_NO_TITLE, null)).thenReturn(OPTION_NO);
        when(connectorTextAccessor.getDescription(OPTION_NO_DESC, null)).thenReturn(OPTION_NO);

        BotAction botAction = botActionBuilder.declineWorkflow(ROUTING_PREFIX, null);
        assertThat(botAction.getTitle()).isEqualTo(OPTION_NO);
        assertThat(botAction.getDescription()).isEqualTo(OPTION_NO);
        assertThat(botAction.getUrl().getHref()).isEqualTo(DECLINE_WORKFLOW_URL);
    }
}
