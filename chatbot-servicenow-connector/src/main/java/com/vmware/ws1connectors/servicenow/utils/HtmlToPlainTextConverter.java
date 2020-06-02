/*
 * Project Service Now Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.servicenow.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import static org.apache.commons.lang3.StringUtils.CR;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.jsoup.internal.StringUtil.in;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HtmlToPlainTextConverter {

    public static String toPlainText(final String htmlContent) {
        if (StringUtils.isBlank(htmlContent)) {
            return htmlContent;
        }
        final HtmlToPlainTextFormatter formatter = new HtmlToPlainTextFormatter();
        NodeTraversor.traverse(formatter, Jsoup.parse(htmlContent));
        return formatter.getFormattedText();
    }

    @SuppressWarnings("PMD.AvoidStringBufferField")
    private static class HtmlToPlainTextFormatter implements NodeVisitor {
        private static final String LI = "li";
        private static final String[] HTML_LINE_FEEDER_ELEMENTS = {"br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5", LI};
        private static final String TAB = "\t";

        private final StringBuilder formattedTextBuilder;

        public HtmlToPlainTextFormatter() {
            formattedTextBuilder = new StringBuilder();
        }

        @Override public void head(Node node, int depth) {
            if (TextNode.class.isInstance(node)) {
                appendText(TextNode.class.cast(node).text());
            } else if (in(node.nodeName(), HTML_LINE_FEEDER_ELEMENTS) && !LI.equals(node.nodeName())) {
                appendText(LF);
            }
        }

        @Override public void tail(Node node, int depth) {
            if (in(node.nodeName(), HTML_LINE_FEEDER_ELEMENTS)) {
                appendText(LF);
            }
        }

        private void appendText(final String text) {
            if (isAccumulatingSpaces(text)) {
                return;
            }
            trimTrailingSpace(text);
            formattedTextBuilder.append(text);
        }

        private boolean isAccumulatingSpaces(final String text) {
            return SPACE.equals(text)
                && (formattedTextBuilder.length() == 0
                || in(formattedTextBuilder.substring(formattedTextBuilder.length() - 1), SPACE, LF, CR, TAB));
        }

        private void trimTrailingSpace(String text) {
            if (LF.equals(text)
                && formattedTextBuilder.length() > 0
                && Character.isSpaceChar(formattedTextBuilder.charAt(formattedTextBuilder.length() - 1))) {
                formattedTextBuilder.deleteCharAt(formattedTextBuilder.length() - 1);
            }
        }

        public String getFormattedText() {
            return formattedTextBuilder.toString().trim();
        }
    }
}
