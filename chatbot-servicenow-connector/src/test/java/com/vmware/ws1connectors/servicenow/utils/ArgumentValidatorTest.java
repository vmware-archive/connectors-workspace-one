/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArgumentValidatorTest {

    private static final String NULL_VALUE = null;
    private static final String VALUE = "value";
    private static final String EMPTY_VALUE = "";
    private static final String FIELD_NAME_NULL_VALUE = "NULL_VALUE";
    private static final String FIELD_NAME_VALUE = "VALUE";

    @Test void throwsIllegalArgumentExceptionForNullArgumentValues() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ArgumentUtils.checkArgumentNotBlank(NULL_VALUE, "NULL_VALUE"));
    }

    @Test void throwsIllegalArgumentExceptionForEmptyArgumentValues() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ArgumentUtils.checkArgumentNotBlank(EMPTY_VALUE, "EMPTY_VALUE"));
    }

    @Test void noExceptionThrownForNonNullValue() {
        assertThatCode(() -> ArgumentUtils.checkArgumentNotBlank(VALUE, "VALUE")).doesNotThrowAnyException();
    }

    @Test void throwsIllegalArgumentExceptionForNullObject() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ArgumentUtils.checkArgumentNotNull(NULL_VALUE, FIELD_NAME_NULL_VALUE));
    }

    @Test void noExceptionThrownForNonNullObject() {
        assertThatCode(() -> ArgumentUtils.checkArgumentNotNull(VALUE, FIELD_NAME_VALUE)).doesNotThrowAnyException();
    }
}
