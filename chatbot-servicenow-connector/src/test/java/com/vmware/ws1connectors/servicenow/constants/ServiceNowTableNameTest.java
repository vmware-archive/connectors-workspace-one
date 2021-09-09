/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.constants;

import com.vmware.ws1connectors.servicenow.utils.ArgumentsStreamBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceNowTableNameTest {

    public static final String CREATE_TICKET_TABLE_NAME = "createTicketTableName";
    public static final String VIEW_TICKET_TABLE_NAME = "viewTicketTableName";
    public static final String INVALID_ENUM_VALUE = "invalid";

    private static Stream<Arguments> withValidInputs() {
        return new ArgumentsStreamBuilder()
                .add(CREATE_TICKET_TABLE_NAME)
                .add(VIEW_TICKET_TABLE_NAME)
                .build();
    }

    private static Stream<Arguments> withInvalidInputs() {
        return new ArgumentsStreamBuilder()
                .add(INVALID_ENUM_VALUE)
                .build();
    }

    @ParameterizedTest
    @MethodSource("withValidInputs")
    void testServiceNowTableNameWithValidInputs(String tableName) {
        assertThat(tableName.equals(ServiceNowTableName.fromServiceNowTableName(tableName).get().getServiceNowTableName()));
    }

    @ParameterizedTest
    @MethodSource("withInvalidInputs")
    void testServiceNowTableNameWithInvalidInputs(String tableName) {
        assertThat(Optional.empty().equals(ServiceNowTableName.fromServiceNowTableName(tableName)));
    }
}
