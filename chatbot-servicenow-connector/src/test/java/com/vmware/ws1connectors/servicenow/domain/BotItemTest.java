/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class BotItemTest {

    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final TabularData TABULAR_DATA_NULL = null;

    @Test public void testAddTabularData() {
        final TabularData expectedTabularData = TabularData.builder()
                .title(TITLE)
                .description(DESCRIPTION)
                .build();
        BotItem botItem = new BotItem.Builder()
                .addTabularData(TabularData.builder()
                        .title(TITLE)
                        .description(DESCRIPTION)
                        .build())
                .build();
        assertThat(botItem.getTabularDataList()).containsExactly(expectedTabularData);
    }

    @Test public void addTabularDataWhenTabularDataIsNull() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new BotItem.Builder().addTabularData(TABULAR_DATA_NULL).build());
    }

}
