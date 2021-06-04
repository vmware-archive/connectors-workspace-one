/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.ws1connectors.workday.models.BusinessTitleChangeTask;
import com.vmware.ws1connectors.workday.models.RequestInfo;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintViolationException;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class BusinessTitleChangeCardBuilderTest extends ControllerTestsBase {

    private static final String ROUTING_PREFIX = "https://dev.hero.example.com/connectors/id/";
    private static final BusinessTitleChangeTask BUSINESS_TITLE_CHANGE_TASK =
            JsonUtils.convertFromJsonFile("business_title_change.json", BusinessTitleChangeTask.class);
    private static final BusinessTitleChangeTask NO_BUSINESS_TITLE_CHANGE_TASK = null;
    //CPD-OFF
    private static final String NO_ROUTING_PREFIX = null;
    private static final Locale NO_LOCALE = null;
    private static final String NO_TENANT_NAME = null;
    private static final String APPROVE = "Approve";
    private static final String DECLINE = "Decline";
    private static final String TENANT_NAME = "vmware_gms";
    private static final String TENANT_URL = "https://impl.workday.com";
    private static final String NO_TENANT_URL = null;
    private static final RequestInfo NO_REQUEST_INFO = null;
    private static final RequestInfo REQUEST_INFO = RequestInfo.builder().tenantName(TENANT_NAME)
            .routingPrefix(ROUTING_PREFIX)
            .tenantUrl(TENANT_URL)
            .isPreHire(false)
            .locale(Locale.ENGLISH)
            .build();

    @Autowired private BusinessTitleChangeCardBuilder businessTitleChangeCardBuilder;

    @Test public void canBuildCard() {
        final Card actualCard = businessTitleChangeCardBuilder.createCard(BUSINESS_TITLE_CHANGE_TASK, REQUEST_INFO);
        final Card expectedCard = JsonUtils.convertFromJsonFile("business_title_change_card.json", Card.class);
        assertThat(actualCard.getBackendId()).isEqualTo(expectedCard.getBackendId());
        assertThat(actualCard.getDueDate()).isEqualTo(expectedCard.getDueDate());
        assertThat(actualCard.getBody()).usingRecursiveComparison().isEqualTo(expectedCard.getBody());
        assertThat(actualCard.getHeader()).usingRecursiveComparison().isEqualTo(expectedCard.getHeader());
        assertThat(actualCard.getActions()).hasSize(2);
        verifyAction(actualCard, expectedCard, APPROVE);
        verifyAction(actualCard, expectedCard, DECLINE);
    }

    private static Stream<Arguments> nullInputsForCreateCard() {
        return new ArgumentsStreamBuilder()
                .add(BUSINESS_TITLE_CHANGE_TASK, NO_REQUEST_INFO)
                .add(NO_BUSINESS_TITLE_CHANGE_TASK, REQUEST_INFO)
                .build();
    }

    @ParameterizedTest
    @MethodSource("nullInputsForCreateCard")
    public void whenCreateCardProvidedWithNullInputs(final BusinessTitleChangeTask businessTitleChangeTask,
                                                     final RequestInfo requestInfo) {
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> businessTitleChangeCardBuilder.createCard(businessTitleChangeTask, requestInfo));
    }

    private static Stream<Arguments> invalidInputsForCreateCard() {
        return new ArgumentsStreamBuilder()
                .add(BUSINESS_TITLE_CHANGE_TASK, RequestInfo.builder().routingPrefix(NO_ROUTING_PREFIX).build())
                .add(BUSINESS_TITLE_CHANGE_TASK, RequestInfo.builder().tenantName(NO_TENANT_NAME).build())
                .add(BUSINESS_TITLE_CHANGE_TASK, RequestInfo.builder().tenantUrl(NO_TENANT_URL).build())
                .add(BUSINESS_TITLE_CHANGE_TASK, RequestInfo.builder().locale(NO_LOCALE).build())
                .build();
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForCreateCard")
    public void whenCreateCardProvidedWithInvalidInputs(final BusinessTitleChangeTask businessTitleChangeTask,
                                                        final RequestInfo requestInfo) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> businessTitleChangeCardBuilder.createCard(businessTitleChangeTask, requestInfo));
    }

    private void verifyAction(final Card actualCard, final Card expectedCard, final String actionLabel) {
        assertThat(actualCard.getActions()).filteredOn(cardAction -> actionLabel.equals(cardAction.getLabel()))
                .first()
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expectedCard.getActions()
                        .stream()
                        .filter(cardAction -> actionLabel.equals(cardAction.getLabel()))
                        .findFirst()
                        .get());
    }
}
