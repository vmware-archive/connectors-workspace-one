/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.domain.snow.Task;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UriBuilderUtilsTest {

    private static final URI NO_BASE_URL = null;
    private static final URI BASE_URL = UriComponentsBuilder.fromUriString("http://localhost:54819/").build().toUri();
    private static final String BASE_URL_WITH_PARAMS = "api/sn_sc/servicecatalog/items/{item_id}/add_to_cart";
    private static final String ITEM_ID = "2ab7077237153000158bbfc8bcbe5da9"; //Macbook pro.
    private static final String PATH = "api/v1/tasks";
    private static final String NO_PATH = null;
    private static final String EXPECTED_BASE_URL = "http://localhost:54819/api/v1/tasks";
    private static final String EXPECTED_BASE_URL_WITH_PARAMS =
            "http://localhost:54819/api/sn_sc/servicecatalog/items/2ab7077237153000158bbfc8bcbe5da9/add_to_cart";
    private static final String BASE_URL_SNOW = "http://localhost:52614/";
    private static final String EXPECTED_TASK_URL = "http://localhost:52614/task.do?sys_id=00909613db113300ea92eb41ca961949";
    private static final String TASK_IMPACT = "3-low";
    private static final String TASK_NO = "TKT0010006";
    private static final String TASK_SYS_ID = "00909613db113300ea92eb41ca961949";
    private static final String TASK_DESCRIPTION = "My mouse is not working.";
    private static final String TASK_STATUS = "open";
    private static final Task NULL_TASK = null;


    @Mock private ExchangeFunction mockExchangeFunc;

    @ParameterizedTest
    @MethodSource("invalidInputsForUriBuilder")
    void whenUriBuilderProvidedWithInvalidInputs(final URI baseUri, final String path) {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> UriBuilderUtils.buildUri(baseUri, path));
        verify(mockExchangeFunc, never()).exchange(any(ClientRequest.class));
    }

    private static Stream<Arguments> invalidInputsForUriBuilder() {
        return new ArgumentsStreamBuilder()
                .add(NO_BASE_URL, NO_PATH)
                .add(BASE_URL, NO_PATH)
                .add(NO_BASE_URL, PATH)
                .build();
    }

    @Test void whenUriBuilderProvidedWithNoBuildArgs() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> UriBuilderUtils.buildUri(BASE_URL, PATH, null));
    }

    @Test void testUriBuilder() {
        assertThat(UriBuilderUtils.buildUri(BASE_URL, PATH)).isEqualTo(EXPECTED_BASE_URL);
    }

    @Test void testUriBuilderWithArgs() {
        assertThat(UriBuilderUtils.buildUri(BASE_URL, BASE_URL_WITH_PARAMS, Map
                .of(ServiceNowConstants.ITEM_ID_STR, ITEM_ID))).isEqualTo(EXPECTED_BASE_URL_WITH_PARAMS);
    }

    @Test void testGetTaskUrl() {
        Task task = getTaskData();
        final Link taskUrl = UriBuilderUtils.getTaskUrl(BASE_URL_SNOW, task);
        assertThat(taskUrl.getHref()).isEqualTo(EXPECTED_TASK_URL);
    }

    private Task getTaskData() {
        Task task = new Task();
        task.setImpact(TASK_IMPACT);
        task.setNumber(TASK_NO);
        task.setSysId(TASK_SYS_ID);
        task.setShortDescription(TASK_DESCRIPTION);
        task.setState(TASK_STATUS);
        return task;
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForGetTaskUrl")
    void testGetTaskUrlWithInvalidInputs(final Task task, final String baseUrl) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> UriBuilderUtils.getTaskUrl(baseUrl, task));
        verify(mockExchangeFunc, never()).exchange(any(ClientRequest.class));
    }

    private static Stream<Arguments> invalidInputsForGetTaskUrl() {
        return new ArgumentsStreamBuilder()
                .add(NULL_TASK, NO_BASE_URL)
                .add(new Task(), NO_BASE_URL)
                .add(new Task(), StringUtils.EMPTY)
                .add(NULL_TASK, BASE_URL_SNOW)
                .build();
    }
}
