/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.utils;

import com.google.common.collect.Lists;
import com.vmware.ws1connectors.workday.models.WorkdayUser;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings
public class ArgumentUtilsTest {
    private static final String NO_BASE_URL = null;
    private static final String NO_WORKDAY_TOKEN = null;
    private static final String NO_TENANT_NAME = null;
    private static final String EMPTY_TENANT_NAME = "";
    private static final String BASE_URL = "http://workday.com";
    private static final String WORKDAY_TOKEN = "workdayToken";
    private static final String TENANT_NAME = "vmware_gms";
    private static final WorkdayUser NO_USER = null;
    private static final WorkdayUser USER = WorkdayUser.builder().build();
    private static final String USER_LABEL = "User";
    private static final String TENANT_LABEL = "Tenant Name";

    private static Stream<Arguments> invalidBasicConnectorArguments() {
        return new ArgumentsStreamBuilder()
            .add(NO_BASE_URL, WORKDAY_TOKEN, TENANT_NAME)
            .add(BASE_URL, NO_WORKDAY_TOKEN, TENANT_NAME)
            .add(BASE_URL, WORKDAY_TOKEN, NO_TENANT_NAME)
            .build();
    }

    @ParameterizedTest
    @MethodSource("invalidBasicConnectorArguments")
    public void whenInvalidBasicConnectorInputsProvided(final String baseUrl, final String workdayAuth, final String tenantName) {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> ArgumentUtils.checkBasicConnectorArgumentsNotBlank(baseUrl, workdayAuth, tenantName));
    }

    @Test public void exceptionNotThrownWhenValidBasicConnectorInputsProvided() {
        assertThatCode(() -> ArgumentUtils.checkBasicConnectorArgumentsNotBlank(BASE_URL, WORKDAY_TOKEN, TENANT_NAME)).doesNotThrowAnyException();
    }

    @Test public void exceptionThrownWhenInputIsNullForCheckArgumentNotNull() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> ArgumentUtils.checkArgumentNotNull(NO_USER, USER_LABEL));

    }

    @Test public void exceptionNotThrownWhenInputIsNotNullForCheckArgumentNotNull() {
        assertThatCode(() -> ArgumentUtils.checkArgumentNotNull(USER, USER_LABEL)).doesNotThrowAnyException();

    }

    private static List<String> blankInputArguments() {
        return Lists.newArrayList(NO_TENANT_NAME, EMPTY_TENANT_NAME, SPACE);
    }

    @ParameterizedTest
    @MethodSource("blankInputArguments")
    public void exceptionThrownWhenStringInputIsBlankForCheckArgumentNotBlank(final String stringInput) {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> ArgumentUtils.checkArgumentNotBlank(stringInput, TENANT_LABEL));
    }

    @Test public void exceptionNotThrownWhenStringInputIsNotBlankForCheckArgumentNotBlank() {
        assertThatCode(() -> ArgumentUtils.checkArgumentNotBlank(TENANT_NAME, TENANT_LABEL)).doesNotThrowAnyException();
    }
}
