/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.test;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.vmware.connectors.utils.IgnoredFieldsReplacer.*;

public final class JsonReplacementsBuilder {

    private static final Logger logger = LoggerFactory.getLogger(JsonReplacementsBuilder.class);

    private final static Configuration configuration = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .build();

    private final String expected;
    private final List<Pair<String, String>> replacements = new ArrayList<>();

    private JsonReplacementsBuilder(String expected) {
        this.expected = expected;
    }

    public static JsonReplacementsBuilder from(String expected) {
        return new JsonReplacementsBuilder(expected);
    }

    public JsonReplacementsBuilder replace(String path, String newValue) {
        replacements.add(Pair.of(path, newValue));
        return this;
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public Matcher<String> buildForCards() {
        return replace("$.cards[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID)
                .replace("$.cards[?(@.creation_date =~ /" + DATE_PATTERN + "/)].creation_date", DUMMY_DATE_TIME)
                .replace("$.cards[?(@.expiration_date =~ /" + DATE_PATTERN + "/)].expiration_date", DUMMY_DATE_TIME)
                .replace("$.cards[*].actions[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID)
                .build(false);
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public Matcher<String> buildForCardsResults() {
        return replace("$.results[?(@.connector_id =~ /" + UUID_PATTERN + "/)].connector_id", DUMMY_UUID)
                .replace("$.results[*].cards[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID)
                .replace("$.results[*].cards[?(@.creation_date =~ /" + DATE_PATTERN + "/)].creation_date", DUMMY_DATE_TIME)
                .replace("$.results[*].cards[*].actions[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID)
                .build(false);
    }

    private class JsonMatcher extends BaseMatcher<String> {

        private final JSONCompareMode compareMode;

        public JsonMatcher(JSONCompareMode compareMode) {
            this.compareMode = compareMode;
        }

        @Override
        public boolean matches(Object item) {
            try {
                JSONCompareResult result = JSONCompare.compareJSON(expected, transform(item.toString()), compareMode);
                if (result.failed()) {
                    logger.error(result.getMessage());
                    return false;
                }
                return true;
            } catch (JSONException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expected);
        }

    }

    public Matcher<String> build(boolean strict) {
        return new JsonMatcher(strict ? JSONCompareMode.STRICT : JSONCompareMode.LENIENT);
    }

    private String transform(String source) {
        DocumentContext context = JsonPath.using(configuration).parse(source);
        replacements.forEach(replacement -> context.set(replacement.getLeft(), replacement.getRight()));
        return context.jsonString();
    }
}
