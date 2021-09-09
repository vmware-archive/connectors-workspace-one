/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.domain;

import com.vmware.connectors.common.payloads.response.Link;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TabularDataTest {
    private static final String TITLE = "title";
    private static final String ID = "00909613db113300ea92eb41ca961949";
    private static final String TYPE_TEXT = "text";
    private static final String TYPE_BUTTON = "text";
    private static final String DESCRIPTION = "description";
    private static final String SHORT_DESCRIPTION = "shortDescription";
    private static final String SUBTITLE = "open";
    private static final String ANOTHER_TITLE = "another_title";
    private static final String ANOTHER_DESCRIPTION = "another_description";
    private static final String URL = "http://localhost:52614/";
    private static final String IMAGE_URL = "http://localhost:52614/ab4537ctr.jpg";
    private static final int TABULAR_DATA_ITEM_LIST_SIZE = 1;

    @Test void testEqualsAndHashCodeWhenObjectsAreEqual() {
        final TabularDataItem tabularDataItem = getTabularDataItem();
        TabularData tabularDataObj1 = TabularData.builder()
                .title(TITLE)
                .description(DESCRIPTION)
                .type(TYPE_TEXT)
                .tabularDataItems(List.of(tabularDataItem))
                .build();
        TabularData tabularDataObj2 = TabularData.builder()
                .title(TITLE)
                .description(DESCRIPTION)
                .type(TYPE_TEXT)
                .tabularDataItems(List.of(tabularDataItem))
                .build();
        assertThat(tabularDataObj1.hashCode()).isEqualTo(tabularDataObj2.hashCode());
        assertThat(tabularDataObj1).isEqualTo(tabularDataObj2);
    }

    private TabularDataItem getTabularDataItem() {
        return TabularDataItem.builder()
                .title(TITLE)
                .shortDescription(SHORT_DESCRIPTION)
                .description(DESCRIPTION)
                .subtitle(SUBTITLE)
                .id(ID)
                .url(new Link(URL))
                .image(new Link(IMAGE_URL))
                .build();
    }

    @Test void testEqualsAndHashCodeWhenObjectsAreNotEqual() {
        TabularData tabularDataObj1 = TabularData.builder()
                .title(TITLE)
                .description(DESCRIPTION)
                .type(TYPE_TEXT)
                .build();
        TabularData tabularDataObj2 = TabularData.builder()
                .title(ANOTHER_TITLE)
                .description(ANOTHER_DESCRIPTION)
                .type(TYPE_BUTTON)
                .build();
        assertThat(tabularDataObj1.hashCode()).isNotEqualTo(tabularDataObj2.hashCode());
        assertThat(tabularDataObj1).isNotEqualTo(tabularDataObj2);
    }

    @Test void testTabularDataObjectBuilder() {
        final TabularDataItem tabularDataItem = getTabularDataItem();
        TabularData tabularData = TabularData.builder()
                .title(TITLE)
                .description(DESCRIPTION)
                .type(TYPE_TEXT)
                .tabularDataItems(List.of(tabularDataItem))
                .build();
        assertThat(tabularData.getTitle()).isEqualTo(TITLE);
        assertThat(tabularData.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(tabularData.getType()).isEqualTo(TYPE_TEXT);
        assertThat(tabularData.getTabularDataItems()).containsExactly(tabularDataItem);
    }
}
