/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.domain.BotAction;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.domain.BotObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import java.util.stream.Stream;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.ITEM_DETAILS;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_CONFIRMATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BotObjectBuilderUtilsTest {

    private static final String ROUTING_PREFIX = "https://mf/connectors/abc123/botDiscovery/";
    private static final String TITLE = "BotItem Title";
    private static final String NO_TITLE = null;
    private static final String DESCRIPTION = "BotItem Description";
    private static final String NO_DESCRIPTION = null;
    private static final int BOT_OBJECT_WITH_SINGLE_ITEM = 1;
    private static final String TYPE = "status";
    private static final String TYPE_TEXT = "text";
    private static final String NO_TYPE = null;
    private static final String OPERATION_CANCEL = "operation.cancel";

    @Mock private ExchangeFunction mockExchangeFunc;
    @Mock private ConnectorTextAccessor connectorTextAccessor;
    @Mock private BotActionBuilder botActionBuilder;

    @ParameterizedTest
    @MethodSource("invalidInputsForBotBuilder")
    public void whenBotItemBuilderProvidedWithInvalidInputs(final String title, final String description, final String type) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> BotObjectBuilderUtils.botObjectBuilder(title, description, WorkflowStep.COMPLETE, type));
        verify(mockExchangeFunc, never()).exchange(any(ClientRequest.class));
    }

    private static Stream<Arguments> invalidInputsForBotBuilder() {
        return new ArgumentsStreamBuilder()
                .add(NO_TITLE, NO_DESCRIPTION, NO_TYPE)
                .add(TITLE, DESCRIPTION, NO_TYPE)
                .add(TITLE, NO_DESCRIPTION, TYPE)
                .add(NO_TITLE, DESCRIPTION, TYPE)
                .add(TITLE, NO_DESCRIPTION, NO_TYPE)
                .add(NO_TITLE, DESCRIPTION, NO_TYPE)
                .add(NO_TITLE, NO_DESCRIPTION, TYPE)
                .build();
    }

    @Test public void whenBotItemBuilderProvidedWithWorkflowStepNull() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> BotObjectBuilderUtils.botObjectBuilder(TITLE, DESCRIPTION, null, TYPE));
    }

    @Test public void testBotBuilderUtils() {
        final BotObjects botObjects = BotObjectBuilderUtils.botObjectBuilder(TITLE, DESCRIPTION, WorkflowStep.COMPLETE, TYPE);
        assertThat(botObjects.getObjects()).hasSize(BOT_OBJECT_WITH_SINGLE_ITEM);
        BotItem botItem = botObjects.getObjects().get(0).get(ITEM_DETAILS);
        assertThat(botItem.getTitle()).isEqualTo(TITLE);
        assertThat(botItem.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(botItem.getType()).isEqualTo(TYPE);
        assertThat(botItem.getWorkflowStep()).isEqualTo(WorkflowStep.COMPLETE);
    }

    @Test public void testConfirmationObject() {
        final String descriptor = "descriptor.string";

        when(connectorTextAccessor.getTitle(descriptor, null)).thenReturn(TITLE);
        when(connectorTextAccessor.getDescription(descriptor, null)).thenReturn(DESCRIPTION);

        BotAction testConfirmAction = new BotAction.Builder()
                .setTitle("confirmActionTitle")
                .setDescription("confirmActionDescription")
                .setUrl(new Link("https://www.vmware.com"))
                .build();

        BotAction testDeclineAction = new BotAction.Builder()
                .setTitle("declineActionTitle")
                .setDescription("declineActionDescription")
                .setUrl(new Link("https://www.evilcorp.com"))
                .build();

        when(botActionBuilder.declineWorkflow(ROUTING_PREFIX, null)).thenReturn(testDeclineAction);

        final BotObjects botObjects = BotObjectBuilderUtils.confirmationObject(connectorTextAccessor, botActionBuilder, descriptor, ROUTING_PREFIX, null, testConfirmAction);

        assertThat(botObjects.getObjects()).hasSize(BOT_OBJECT_WITH_SINGLE_ITEM);

        BotItem botItem = botObjects.getObjects().get(0).get(ITEM_DETAILS);
        BotAction confirmAction = botItem.getActions().get(0);
        BotAction declineAction = botItem.getActions().get(1);

        assertThat(botItem.getTitle()).isEqualTo(TITLE);
        assertThat(botItem.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(botItem.getType()).isEqualTo(TYPE_TEXT);
        assertThat(botItem.getWorkflowStep()).isEqualTo(WorkflowStep.INCOMPLETE);

        assertThat(confirmAction.getTitle()).isEqualTo("confirmActionTitle");
        assertThat(confirmAction.getDescription()).isEqualTo("confirmActionDescription");
        assertThat(confirmAction.getUrl().getHref()).isEqualTo("https://www.vmware.com");

        assertThat(declineAction.getTitle()).isEqualTo("declineActionTitle");
        assertThat(declineAction.getDescription()).isEqualTo("declineActionDescription");
        assertThat(declineAction.getUrl().getHref()).isEqualTo("https://www.evilcorp.com");
    }

    @Test public void testConfirmationObjectNullTextAccessor() {
        final String descriptor = "placeholder";

        BotAction testConfirmAction = new BotAction.Builder()
                .setTitle("confirmActionTitle")
                .setDescription("confirmActionDescription")
                .setUrl(new Link("https://www.vmware.com"))
                .build();

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> BotObjectBuilderUtils.confirmationObject(
                        null,
                        botActionBuilder,
                        descriptor,
                        ROUTING_PREFIX,
                        null,
                        testConfirmAction));
    }

    @Test public void testConfirmationObjectNullActionBuilderAccessor() {
        final String descriptor = "placeholder";

        BotAction testConfirmAction = new BotAction.Builder()
                .setTitle("confirmActionTitle")
                .setDescription("confirmActionDescription")
                .setUrl(new Link("https://www.vmware.com"))
                .build();

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> BotObjectBuilderUtils.confirmationObject(
                connectorTextAccessor,
                        null,
                        descriptor,
                        ROUTING_PREFIX,
                        null,
                        testConfirmAction));
    }

    @Test public void testCancelObject() {
        when(connectorTextAccessor.getTitle(OPERATION_CANCEL, null)).thenReturn(TITLE);
        when(connectorTextAccessor.getDescription(OPERATION_CANCEL, null)).thenReturn(DESCRIPTION);

        final BotObjects botObjects = BotObjectBuilderUtils.cancelObject(connectorTextAccessor, null);

        assertThat(botObjects.getObjects()).hasSize(BOT_OBJECT_WITH_SINGLE_ITEM);

        BotItem botItem = botObjects.getObjects().get(0).get(ITEM_DETAILS);

        assertThat(botItem.getTitle()).isEqualTo(TITLE);
        assertThat(botItem.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(botItem.getType()).isEqualTo(UI_TYPE_CONFIRMATION);
        assertThat(botItem.getWorkflowStep()).isEqualTo(WorkflowStep.COMPLETE);
    }

    @Test public void testCancelObjectNullTextAccessor() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> BotObjectBuilderUtils.cancelObject(null, null));
    }
}