/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HashUtilTest {

    @Test
    void hash() {
        assertEquals(HashUtil.hash("a", "b", "c"), HashUtil.hash("a", "b", "c"));
        assertNotEquals(HashUtil.hash("X", "b", "c"), HashUtil.hash("a", "b", "c"));
        assertNotEquals(HashUtil.hash("a", "X", "c"), HashUtil.hash("a", "b", "c"));
        assertNotEquals(HashUtil.hash("a", "b", "X"), HashUtil.hash("a", "b", "c"));
        // TODO - mix numbers and strings, make sure toString isn't used? (but a disabled on that test, we don't need it yet)
    }

    @ParameterizedTest
    @MethodSource("hashListTestArgProvider")
    void hashList(List<String> a1, List<String> a2, boolean shouldBeEqual) {
        String h1 = HashUtil.hashList(a1);
        String h2 = HashUtil.hashList(a2);
        if (shouldBeEqual) {
            assertEquals(h1, h2);
        } else {
            assertNotEquals(h1, h2);
        }
    }

    private static Stream<Arguments> hashListTestArgProvider() {
        return Stream.of(
                Arguments.of(null, null, true),
                Arguments.of(List.of(), List.of(), true),
                Arguments.of(List.of("a"), List.of("a"), true),
                Arguments.of(List.of("a", "b"), List.of("a", "b"), true),
                Arguments.of(List.of("a", "b", "c"), List.of("a", "b", "c"), true),
                Arguments.of(List.of("a", "", "c"), List.of("a", "", "c"), true),
                Arguments.of(List.of("", "", ""), List.of("", "", ""), true),
                // simple differences
                Arguments.of(null, List.of(), false),
                Arguments.of(List.of("X", "b", "c"), List.of("a", "b", "c"), false),
                Arguments.of(List.of("a", "X", "c"), List.of("a", "b", "c"), false),
                Arguments.of(List.of("a", "b", "X"), List.of("a", "b", "c"), false),
                // tricky differences
                // make sure it doesn't fully rely on List's toString
                Arguments.of(List.of("a,b", "c"), List.of("a", "b", "c"), false),
                Arguments.of(List.of("a, b", "c"), List.of("a", "b", "c"), false),
                // make sure it doesn't fully rely on separators
                Arguments.of(List.of("a|b", "c"), List.of("a", "b", "c"), false),
                Arguments.of(List.of("a | b", "c"), List.of("a", "b", "c"), false),
                Arguments.of(List.of("a;b", "c"), List.of("a", "b", "c"), false),
                Arguments.of(List.of("a ; b", "c"), List.of("a", "b", "c"), false),
                Arguments.of(List.of("a ", "b c"), List.of("a", "b", "c"), false),
                // make sure it doesn't rely on space separator and trimming the input
                Arguments.of(List.of("a  b", "", "c"), List.of("a", "", "b  c"), false),
                // make sure it handles different size empty content
                Arguments.of(List.of("", ""), List.of("", "", ""), false)
        );
    }

    @ParameterizedTest
    @MethodSource("hashMapTestArgProvider")
    void hashMap(Map<String, String> m1, Map<String, String> m2, boolean shouldBeEqual) {
        String h1 = HashUtil.hashMap(m1);
        String h2 = HashUtil.hashMap(m2);
        if (shouldBeEqual) {
            assertEquals(h1, h2);
        } else {
            assertNotEquals(h1, h2);
        }
    }

    private static Stream<Arguments> hashMapTestArgProvider() {
        return Stream.of(
                Arguments.of(null, null, true),
                Arguments.of(Map.of(), Map.of(), true),
                Arguments.of(Map.of("a", "1"), Map.of("a", "1"), true),
                Arguments.of(Map.of("a", "1", "b", "2"), Map.of("a", "1", "b", "2"), true),
                Arguments.of(Map.of("a", "1", "b", ""), Map.of("a", "1", "b", ""), true),
                // simple differences
                Arguments.of(null, Map.of(), false),
                Arguments.of(Map.of("a", "1"), Map.of("a", "1", "b", "2"), false),
                Arguments.of(Map.of("a", "X", "b", "2"), Map.of("a", "1", "b", "2"), false),
                Arguments.of(Map.of("a", "1", "b", "X"), Map.of("a", "1", "b", "2"), false),
                // tricky differences
                // make sure it includes the key and value in the map
                Arguments.of(Map.of("a", "1", "c", "1"), Map.of("a", "1", "b", "1"), false),
                // make sure it doesn't fully rely on Map's toString
                Arguments.of(Map.of("a", "1, b=2"), Map.of("a", "1", "b", "2"), false)
        );
    }

}
