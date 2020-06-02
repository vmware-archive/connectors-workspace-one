/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BotItemBuilderTest {

    private static final String TITLE = "BotItem Title";
    private static final String NO_TITLE = null;
    private static final String DESCRIPTION = "BotItem Description";
    private static final String NO_DESCRIPTION = null;
    private static final int BOT_OBJECT_WITH_SINGLE_ITEM = 1;
    private static final String TYPE = "status";
    private static final String NO_TYPE = null;
    @Mock private ExchangeFunction mockExchangeFunc;

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
}