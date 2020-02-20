/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CardHeaderTest {

    @ParameterizedTest
    @MethodSource("hashTestArgProvider")
    void hash(String t1, String t2, List<String> sub1, List<String> sub2, CardHeaderLinks links1, CardHeaderLinks links2, boolean shouldBeEqual) {
        CardHeader h1 = new CardHeader(t1, sub1, links1);
        CardHeader h2 = new CardHeader(t2, sub2, links2);
        if (shouldBeEqual) {
            assertEquals(h1.hash(), h2.hash());
        } else {
            assertNotEquals(h1.hash(), h2.hash());
        }
    }

    private static Stream<Arguments> hashTestArgProvider() {
        var links1 = new CardHeaderLinks("https://same.com/t", null);
        var links2 = new CardHeaderLinks("https://same.com/t", null);
        var links3 = new CardHeaderLinks("https://different.com/t", null);
        return Stream.of(
                Arguments.of("t", "t", null, null, null, null, true),
                Arguments.of("t", "t", List.of(), List.of(), links1, links2, true),
                Arguments.of("t", "t", List.of("a"), List.of("a"), links1, links2, true),
                Arguments.of("t", "t", List.of("a", "b"), List.of("a", "b"), links1, links2, true),
                Arguments.of("t", "t", List.of("a", "b", "c"), List.of("a", "b", "c"), links1, links2, true),
                Arguments.of("", "", List.of("a", "b", "c"), List.of("a", "b", "c"), links1, links2, true),
                Arguments.of("t", "t", List.of("a", "", "c"), List.of("a", "", "c"), links1, links2, true),
                Arguments.of("t", "t", List.of("", "", ""), List.of("", "", ""), links1, links2, true),
                // simple differences
                Arguments.of("t", "t", null, List.of(), links1, links2, false),
                Arguments.of("t", "X", List.of("a", "b", "c"), List.of("a", "b", "c"), links1, links2, false),
                Arguments.of("t", "t", List.of("X", "b", "c"), List.of("a", "b", "c"), links1, links2, false),
                Arguments.of("t", "t", List.of("a", "X", "c"), List.of("a", "b", "c"), links1, links2, false),
                Arguments.of("t", "t", List.of("a", "b", "X"), List.of("a", "b", "c"), links1, links2, false),
                Arguments.of("t", "t", List.of("a", "b", "c"), List.of("a", "b", "c"), links1, links3, false),
                // tricky differences
                // make sure it doesn't fully rely on List's toString
                Arguments.of("t", "t", List.of("a,b", "c"), List.of("a", "b", "c"), links1, links2, false),
                Arguments.of("t", "t", List.of("a, b", "c"), List.of("a", "b", "c"), links1, links2, false),
                // make sure it doesn't fully rely on separators
                Arguments.of("t", "t", List.of("a|b", "c"), List.of("a", "b", "c"), links1, links2, false),
                Arguments.of("t", "t", List.of("a | b", "c"), List.of("a", "b", "c"), links1, links2, false),
                Arguments.of("t", "t", List.of("a;b", "c"), List.of("a", "b", "c"), links1, links2, false),
                Arguments.of("t", "t", List.of("a; b", "c"), List.of("a", "b", "c"), links1, links2, false),
                Arguments.of("t", "t", List.of("a ", "b c"), List.of("a", "b", "c"), links1, links2, false),
                // make sure it doesn't rely on space separator and trimming the input
                Arguments.of("t", "t", List.of("a  b", "", "c"), List.of("a", "", "b  c"), links1, links2, false),
                // make sure it handles different size empty content
                Arguments.of("t", "t", List.of("", ""), List.of("", "", ""), links1, links2, false)
        );
    }

}
