/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.ws1connectors.servicenow.domain.TabularData;
import com.vmware.ws1connectors.servicenow.domain.TabularDataItem;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.IMPACT;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SHORT_DESCRIPTION;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.STATUS;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SYS_ID;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.TASK_DO;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.TICKET_NO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TabularDataBuilderUtilsTest {

    private static final String BASE_URL_SNOW = "http://localhost:52614/";
    private static final String TASK_IMPACT = "3-low";
    private static final String TASK_NO = "TKT0010006";
    private static final String TASK_SYS_ID = "00909613db113300ea92eb41ca961949";
    private static final String TASK_DESCRIPTION = "My mouse is not working.";
    private static final String TASK_STATUS = "open";
    private static final URI NO_BASE_URL = null;
    private static final Task NULL_TASK = null;

    @Mock private ExchangeFunction mockExchangeFunc;

    @Test void testBuildTabularDataForTask() {
        Task task = getTaskData();
        TabularData expectedTabularData = getExpectedTabularDataObj(task);
        final TabularData tabularData = TabularDataBuilderUtils.buildTabularDataForTask(task, BASE_URL_SNOW);
        assertThat(tabularData.getTabularDataItems()).hasSize(4);
        assertThat(tabularData).isEqualTo(expectedTabularData);
    }

    private TabularData getExpectedTabularDataObj(Task task) {
        List<TabularDataItem> tabularDataItems = new ArrayList<>();
        tabularDataItems.add(buildTabularDataItem(IMPACT, task.getImpact()));
        tabularDataItems.add(buildTabularDataItem(STATUS, task.getState()));
        tabularDataItems.add(buildTabularDataItem(SHORT_DESCRIPTION, task.getShortDescription()));
        tabularDataItems.add(TabularDataItem.builder().title(TICKET_NO).shortDescription(task.getNumber()).url(new Link(
                UriComponentsBuilder.fromUriString(BASE_URL_SNOW)
                        .replacePath(TASK_DO)
                        .queryParam(SYS_ID, task.getSysId())
                        .build()
                        .toUriString()
        )).build());
        return TabularData.builder().tabularDataItems(tabularDataItems).build();
    }

    private TabularDataItem buildTabularDataItem(String title, String shortDescription) {
        return TabularDataItem.builder().title(title).shortDescription(shortDescription).build();
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
    @MethodSource("invalidInputsForBuildTabularDataForTask")
    void testBuildTabularDataForTaskWithInvalidInputs(final Task task, final String baseUrl) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> TabularDataBuilderUtils.buildTabularDataForTask(task, baseUrl));
        verify(mockExchangeFunc, never()).exchange(any(ClientRequest.class));
    }

    private static Stream<Arguments> invalidInputsForBuildTabularDataForTask() {
        return new ArgumentsStreamBuilder()
                .add(NULL_TASK, NO_BASE_URL)
                .add(new Task(), NO_BASE_URL)
                .add(new Task(), StringUtils.EMPTY)
                .add(NULL_TASK, BASE_URL_SNOW)
                .build();
    }

}
