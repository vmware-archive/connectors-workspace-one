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
    void hash(String t1, String t2, List<String> sub1, List<String> sub2, boolean shouldBeEqual) {
        CardHeader h1 = new CardHeader(t1, sub1);
        CardHeader h2 = new CardHeader(t2, sub2);
        if (shouldBeEqual) {
            assertEquals(h1.hash(), h2.hash());
        } else {
            assertNotEquals(h1.hash(), h2.hash());
        }
    }

    private static Stream<Arguments> hashTestArgProvider() {
        return Stream.of(
                Arguments.of("t", "t", null, null, true),
                Arguments.of("t", "t", List.of(), List.of(), true),
                Arguments.of("t", "t", List.of("a"), List.of("a"), true),
                Arguments.of("t", "t", List.of("a", "b"), List.of("a", "b"), true),
                Arguments.of("t", "t", List.of("a", "b", "c"), List.of("a", "b", "c"), true),
                Arguments.of("", "", List.of("a", "b", "c"), List.of("a", "b", "c"), true),
                Arguments.of("t", "t", List.of("a", "", "c"), List.of("a", "", "c"), true),
                Arguments.of("t", "t", List.of("", "", ""), List.of("", "", ""), true),
                // simple differences
                Arguments.of("t", "t", null, List.of(), false),
                Arguments.of("t", "X", List.of("a", "b", "c"), List.of("a", "b", "c"), false),
                Arguments.of("t", "t", List.of("X", "b", "c"), List.of("a", "b", "c"), false),
                Arguments.of("t", "t", List.of("a", "X", "c"), List.of("a", "b", "c"), false),
                Arguments.of("t", "t", List.of("a", "b", "X"), List.of("a", "b", "c"), false),
                // tricky differences
                // make sure it doesn't fully rely on List's toString
                Arguments.of("t", "t", List.of("a,b", "c"), List.of("a", "b", "c"), false),
                Arguments.of("t", "t", List.of("a, b", "c"), List.of("a", "b", "c"), false),
                // make sure it doesn't fully rely on separators
                Arguments.of("t", "t", List.of("a|b", "c"), List.of("a", "b", "c"), false),
                Arguments.of("t", "t", List.of("a | b", "c"), List.of("a", "b", "c"), false),
                Arguments.of("t", "t", List.of("a;b", "c"), List.of("a", "b", "c"), false),
                Arguments.of("t", "t", List.of("a; b", "c"), List.of("a", "b", "c"), false),
                Arguments.of("t", "t", List.of("a ", "b c"), List.of("a", "b", "c"), false),
                // make sure it doesn't rely on space separator and trimming the input
                Arguments.of("t", "t", List.of("a  b", "", "c"), List.of("a", "", "b  c"), false),
                // make sure it handles different size empty content
                Arguments.of("t", "t", List.of("", ""), List.of("", "", ""), false)
        );
    }

}
