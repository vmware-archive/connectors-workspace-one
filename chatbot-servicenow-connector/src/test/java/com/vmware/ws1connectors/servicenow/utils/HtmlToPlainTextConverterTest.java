/*
 * Project Service Now Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.servicenow.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.assertj.core.api.Assertions.assertThat;

class HtmlToPlainTextConverterTest {
    private static final String NO_HTML_CONTENT = null;

    @Test void formatsNullHtmlContent() {
        formatsEmptyHtmlContent(NO_HTML_CONTENT);
    }

    @ParameterizedTest
    @ValueSource(strings = {SPACE, LF, EMPTY})
    void formatsEmptyHtmlContent(final String htmlContent) {
        assertThat(HtmlToPlainTextConverter.toPlainText(htmlContent)).isEqualTo(htmlContent);
    }

    @ParameterizedTest
    @MethodSource("getInputsForFormatHtmlContent")
    void formatsHtmlContent(final String html, final String text) {
        final String actualPlainText = HtmlToPlainTextConverter.toPlainText(FileUtils.readFileAsString(html));
        assertThat(actualPlainText).isEqualTo(FileUtils.readFileAsString(text));
    }

    private static Stream<Arguments> getInputsForFormatHtmlContent() {
        return new ArgumentsStreamBuilder()
            .add("html/mac_pro_description.html", "text/mac_pro_description.text")
            .add("html/mac_pro_malformed_description.html", "text/mac_pro_description_anchor.text")
            .add("html/mac_pro_malformed_2_description.html", "text/mac_pro_malformed_2_description.text")
            .add("html/mac_pro_description_anchor.html", "text/mac_pro_description_anchor.text")
            .build();
    }
}
