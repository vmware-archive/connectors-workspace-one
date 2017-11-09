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

import java.util.ArrayList;
import java.util.List;


public final class JsonReplacementsBuilder {
    /**
     * A Pattern that matches Strings in UUID format. Version number is not checked, so invalid UUIDs can match
     * against this pattern.
     */
    private final static String UUID_PATTERN = "[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}";

    /**
     * A Pattern that matches Strings in date format (2017-05-06T07:10:34.000+00:00)
     */
    private final static String DATE_PATTERN = "^([\\+-]?\\d{4}(?!\\d{2}\\b))((-?)((0[1-9]|1[0-2])(\\3([12]\\d|0[1-9]|3[01]))?|W([0-4]\\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\\d|[12]\\d{2}|3([0-5]\\d|6[1-6])))([T\\s]((([01]\\d|2[0-3])((:?)[0-5]\\d)?|24\\:?00)([\\.,]\\d+(?!:))?)?(\\17[0-5]\\d([\\.,]\\d+)?)?([zZ]|([\\+-])([01]\\d|2[0-3]):?([0-5]\\d)?)?)?)?$";
    /**
     * A well-formed UUID string containing all zeroes. This is actually not a valid UUID because it does not have
     * a valid version number.
     */
    private static final String DUMMY_UUID = "00000000-0000-0000-0000-000000000000";


    /**
     * A well-formed ISO 8601 compliant Date
     */
    private static final String DUMMY_DATE_TIME = "1970-01-01T00:00:00Z";

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

    public Matcher<String> build(boolean strict) {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object item) {
                try {
                    JSONCompareResult result = JSONCompare.compareJSON(expected, transform(item.toString()),
                            strict ? JSONCompareMode.STRICT : JSONCompareMode.LENIENT);
                    if (result.failed()) {
                        System.err.println(result.getMessage());
                        return false;
                    } else {
                        return true;
                    }
                } catch (JSONException e) {
                    throw new AssertionError(e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(expected);
            }
        };
    }

    private String transform(String source) {
        DocumentContext context = JsonPath.using(configuration).parse(source);
        replacements.forEach(replacement -> context.set(replacement.getLeft(), replacement.getRight()));
        return context.jsonString();
    }
}
