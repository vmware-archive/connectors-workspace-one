/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.connectors.common.payloads.response;

import com.vmware.connectors.common.utils.ArgumentsStreamBuilder;
import com.vmware.connectors.common.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class OpenInLinkTest {
    private static final URI TEST_HREF =
            UriComponentsBuilder.fromUriString("https://impl.dummyserver.com").build().toUri();
    private static final URI ANOTHER_TEST_HREF =
            UriComponentsBuilder.fromUriString("https://impl.dummyserver1.com").build().toUri();
    private static final String TEXT = "Open In dummy Server";
    private static final String ANOTHER_TEXT = "Open In dummy Server1";
    private static final URI NO_HREF = null;
    private static final String NO_TEXT = null;

    @Test public void testBuilder() {
        final OpenInLink openInApp = OpenInLink.builder()
                .href(TEST_HREF)
                .text(TEXT)
                .build();
        assertGetters(openInApp);
    }

    @Test public void testSetters() {
        final OpenInLink openInApp = OpenInLink.builder().build();
        openInApp.setHref(TEST_HREF);
        openInApp.setText(TEXT);
        assertGetters(openInApp);
    }

    @Test public void testJsonDeserialize() {
        OpenInLink openInLink = JsonUtils.convertFromJsonFile("open_in_link.json", OpenInLink.class);
        assertGetters(openInLink);
    }

    @ParameterizedTest
    @MethodSource("getOpenInLinkArguments")
    public void testHash(final URI href1, final URI href2, final String text1, final String text2, boolean expectEqual) {
        final OpenInLink openInApp1 = OpenInLink.builder()
                .href(href1)
                .text(text1)
                .build();
        final OpenInLink openInApp2 = OpenInLink.builder()
                .href(href2)
                .text(text2)
                .build();

        if (expectEqual) {
            assertEquals(openInApp1.hash(), openInApp2.hash());
        } else {
            assertNotEquals(openInApp1.hash(), openInApp2.hash());
        }
    }

    private static Stream<Arguments> getOpenInLinkArguments() {
        return new ArgumentsStreamBuilder()
                .add(TEST_HREF, TEST_HREF, TEXT, TEXT, true)
                .add(NO_HREF, NO_HREF, NO_TEXT, NO_TEXT, true)
                .add(NO_HREF, NO_HREF, TEXT, TEXT, true)
                .add(TEST_HREF, TEST_HREF, NO_TEXT, NO_TEXT, true)
                .add(NO_HREF, TEST_HREF, TEXT, TEXT, false)
                .add(TEST_HREF, TEST_HREF, NO_TEXT, TEXT, false)
                .add(TEST_HREF, ANOTHER_TEST_HREF, TEXT, TEXT, false)
                .add(ANOTHER_TEST_HREF, ANOTHER_TEST_HREF, TEXT, ANOTHER_TEXT, false)
                .add(TEST_HREF, ANOTHER_TEST_HREF, TEXT, ANOTHER_TEXT, false)
                .build();
    }

    private void assertGetters(OpenInLink openInApp) {
        assertEquals(openInApp.getHref(), TEST_HREF);
        assertEquals(openInApp.getText(), TEXT);
    }
}
