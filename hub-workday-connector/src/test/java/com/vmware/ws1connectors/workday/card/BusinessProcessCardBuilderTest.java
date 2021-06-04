/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.ws1connectors.workday.models.BusinessProcessTask;
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

public class BusinessProcessCardBuilderTest extends ControllerTestsBase {
    private static final String ROUTING_PREFIX = "https://dev.hero.example.com/connectors/id/";
    private static final BusinessProcessTask BUSINESS_PROCESS_TASK_1 =
            JsonUtils.convertFromJsonFile("Business_Process_Details.json", BusinessProcessTask.class);
    private static final BusinessProcessTask BUSINESS_PROCESS_TASK_2 =
            JsonUtils.convertFromJsonFile("Business_Process_Details_1.json", BusinessProcessTask.class);
    private static final BusinessProcessTask BUSINESS_PROCESS_TASK_WITH_STEP_TYPE_ACTION =
            JsonUtils.convertFromJsonFile("Business_Process_Details_Step_Type_Action.json", BusinessProcessTask.class);
    private static final BusinessProcessTask BUSINESS_PROCESS_TASK_FOR_PRE_HIRE =
            JsonUtils.convertFromJsonFile("Business_Process_Details_Pre_Hire_1.json", BusinessProcessTask.class);
    private static final BusinessProcessTask NO_BUSINESS_PROCESS_TASK = null;
    private static final String NO_ROUTING_PREFIX = null;
    private static final Locale NO_LOCALE = null;
    private static final String NO_TENANT_NAME = null;
    private static final String NO_TENANT_URL = null;
    private static final String APPROVE = "Approve";
    private static final String DECLINE = "Decline";
    private static final String VIEW_IN_WORKDAY = "View In Workday";
    private static final String TENANT_NAME = "vmware_gms";
    private static final String TENANT_URL = "https://impl.workday.com";
    private static final RequestInfo NO_REQUEST_INFO = null;
    private static final RequestInfo REQUEST_INFO = RequestInfo.builder().tenantName(TENANT_NAME)
            .routingPrefix(ROUTING_PREFIX)
            .tenantUrl(TENANT_URL)
            .isPreHire(false)
            .locale(Locale.ENGLISH)
            .build();

    @Autowired private BusinessProcessCardBuilder businessProcessCardBuilder;

    private static Stream<Arguments> getBusinessProcessTaskArguments() {
        return new ArgumentsStreamBuilder()
                .add(BUSINESS_PROCESS_TASK_1, "Business_Process_Card_No_Due_Date.json")
                .add(BUSINESS_PROCESS_TASK_2, "Business_Process_Card_With_Due_Date.json")
                .build();
    }

    @ParameterizedTest
    @MethodSource("getBusinessProcessTaskArguments")
    public void canBuildCard(final BusinessProcessTask businessProcessTask, final String expectedCardJson) {
        final Card actualCard = businessProcessCardBuilder.createCard(businessProcessTask, REQUEST_INFO);
        //CPD-OFF
        final Card expectedCard = JsonUtils.convertFromJsonFile(expectedCardJson, Card.class);
        assertThat(actualCard.getBackendId()).isEqualTo(expectedCard.getBackendId());
        assertThat(actualCard.getDueDate()).isEqualTo(expectedCard.getDueDate());
        assertThat(actualCard.getBody()).usingRecursiveComparison().isEqualTo(expectedCard.getBody());
        assertThat(actualCard.getHeader()).usingRecursiveComparison().isEqualTo(expectedCard.getHeader());
        assertThat(actualCard.getActions()).hasSize(2);
        verifyAction(actualCard, expectedCard, APPROVE);
        verifyAction(actualCard, expectedCard, DECLINE);
    }

    @Test public void canBuildCardWithInboxTaskWhenStepTypeIsAction() {
        final Card actualCard = businessProcessCardBuilder.createCard(BUSINESS_PROCESS_TASK_WITH_STEP_TYPE_ACTION, REQUEST_INFO);
        //CPD-OFF
        final Card expectedCard = JsonUtils.convertFromJsonFile("Business_Process_Card_Step_Type_Action.json", Card.class);
        assertThat(actualCard.getBackendId()).isEqualTo(expectedCard.getBackendId());
        assertThat(actualCard.getDueDate()).isEqualTo(expectedCard.getDueDate());
        assertThat(actualCard.getBody()).usingRecursiveComparison().isEqualTo(expectedCard.getBody());
        assertThat(actualCard.getHeader()).usingRecursiveComparison().isEqualTo(expectedCard.getHeader());
        assertThat(actualCard.getActions()).hasSize(1);
        verifyAction(actualCard, expectedCard, VIEW_IN_WORKDAY);
    }

    @Test public void canBuildCardWithInboxTaskForPreHire() {
        final RequestInfo requestInfoPreHire = RequestInfo.builder().tenantName(TENANT_NAME)
                .routingPrefix(ROUTING_PREFIX)
                .tenantUrl(TENANT_URL)
                .isPreHire(true)
                .locale(Locale.ENGLISH)
                .build();
        final Card actualCard = businessProcessCardBuilder.createCard(BUSINESS_PROCESS_TASK_FOR_PRE_HIRE, requestInfoPreHire);
        //CPD-OFF
        final Card expectedCard = JsonUtils.convertFromJsonFile("cards/business_process_card_pre_hire.json", Card.class);
        assertThat(actualCard.getBackendId()).isEqualTo(expectedCard.getBackendId());
        assertThat(actualCard.getDueDate()).isEqualTo(expectedCard.getDueDate());
        assertThat(actualCard.getBody()).usingRecursiveComparison().isEqualTo(expectedCard.getBody());
        assertThat(actualCard.getHeader()).usingRecursiveComparison().isEqualTo(expectedCard.getHeader());
        assertThat(actualCard.getActions()).hasSize(1);
        verifyAction(actualCard, expectedCard, VIEW_IN_WORKDAY);
    }

    private static Stream<Arguments> nullInputsForCreateCard() {
        return new ArgumentsStreamBuilder()
                .add(BUSINESS_PROCESS_TASK_1, NO_REQUEST_INFO)
                .add(NO_BUSINESS_PROCESS_TASK, REQUEST_INFO)
                .build();
    }

    @ParameterizedTest
    @MethodSource("nullInputsForCreateCard")
    public void whenCreateCardProvidedWithNullInputs(final BusinessProcessTask businessProcessTask,
                                                        final RequestInfo requestInfo) {
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> businessProcessCardBuilder.createCard(businessProcessTask, requestInfo));
    }

    private static Stream<Arguments> invalidInputsForCreateCard() {
        return new ArgumentsStreamBuilder()
                .add(BUSINESS_PROCESS_TASK_1, RequestInfo.builder().routingPrefix(NO_ROUTING_PREFIX).build())
                .add(BUSINESS_PROCESS_TASK_1, RequestInfo.builder().tenantName(NO_TENANT_NAME).build())
                .add(BUSINESS_PROCESS_TASK_1, RequestInfo.builder().tenantUrl(NO_TENANT_URL).build())
                .add(BUSINESS_PROCESS_TASK_1, RequestInfo.builder().locale(NO_LOCALE).build())
                .build();
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForCreateCard")
    public void whenCreateCardProvidedWithInvalidInputs(final BusinessProcessTask businessProcessTask,
                                                        final RequestInfo requestInfo) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> businessProcessCardBuilder.createCard(businessProcessTask, requestInfo));
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
    //CPD-ON
}
