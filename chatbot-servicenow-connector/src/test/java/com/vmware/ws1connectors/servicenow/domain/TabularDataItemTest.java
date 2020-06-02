/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.domain;

import com.vmware.connectors.common.payloads.response.Link;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TabularDataItemTest {

    private static final String TITLE = "title";
    private static final String ID = "00909613db113300ea92eb41ca961949";
    private static final String DESCRIPTION = "description";
    private static final String SHORT_DESCRIPTION = "shortDescription";
    private static final String SUBTITLE = "open";
    private static final String ANOTHER_TITLE = "another_title";
    private static final String ANOTHER_DESCRIPTION = "another_description";
    private static final String ANOTHER_SHORT_DESCRIPTION = "another_shortDescription";
    private static final String SUBTITLE_CLOSED = "close";
    private static final String URL = "http://localhost:52614/";
    private static final String IMAGE_URL = "http://localhost:52614/ab4537ctr.jpg";

    @Test public void testEqualsAndHashCodeWhenObjectsAreEqual() {
        TabularDataItem tabularDataItemObj1 = TabularDataItem.builder()
                .title(TITLE)
                .description(DESCRIPTION)
                .shortDescription(SHORT_DESCRIPTION)
                .subtitle(SUBTITLE)
                .build();
        TabularDataItem tabularDataItemObj2 = TabularDataItem.builder()
                .title(TITLE)
                .description(DESCRIPTION)
                .shortDescription(SHORT_DESCRIPTION)
                .subtitle(SUBTITLE)
                .build();
        assertThat(tabularDataItemObj1.hashCode()).isEqualTo(tabularDataItemObj2.hashCode());
        assertThat(tabularDataItemObj1).isEqualTo(tabularDataItemObj2);
    }

    @Test public void testEqualsAndHashCodeWhenObjectsAreNotEqual() {
        TabularDataItem tabularDataItemObj1 = TabularDataItem.builder()
                .title(TITLE)
                .description(DESCRIPTION)
                .shortDescription(SHORT_DESCRIPTION)
                .subtitle(SUBTITLE)
                .build();
        TabularDataItem tabularDataItemObj2 = TabularDataItem.builder()
                .title(ANOTHER_TITLE)
                .description(ANOTHER_DESCRIPTION)
                .shortDescription(ANOTHER_SHORT_DESCRIPTION)
                .subtitle(SUBTITLE_CLOSED)
                .build();
        assertThat(tabularDataItemObj1.hashCode()).isNotEqualTo(tabularDataItemObj2.hashCode());
        assertThat(tabularDataItemObj1).isNotEqualTo(tabularDataItemObj2);
    }

    @Test public void testTabularDataItemObjectBuilder() {
        TabularDataItem tabularDataItem = TabularDataItem.builder()
                .title(TITLE)
                .shortDescription(SHORT_DESCRIPTION)
                .description(DESCRIPTION)
                .subtitle(SUBTITLE)
                .id(ID)
                .url(new Link(URL))
                .image(new Link(IMAGE_URL))
                .build();
        verifyTabaularDataItemData(tabularDataItem);
    }

    private void verifyTabaularDataItemData(TabularDataItem tabularDataItem) {
        assertThat(tabularDataItem.getId()).isEqualTo(ID);
        assertThat(tabularDataItem.getTitle()).isEqualTo(TITLE);
        assertThat(tabularDataItem.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(tabularDataItem.getShortDescription()).isEqualTo(SHORT_DESCRIPTION);
        assertThat(tabularDataItem.getSubtitle()).isEqualTo(SUBTITLE);
        assertThat(tabularDataItem.getImage().getHref()).isEqualTo(IMAGE_URL);
        assertThat(tabularDataItem.getUrl().getHref()).isEqualTo(URL);
    }
}
