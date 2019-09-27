/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CardActionInputFieldTest {

    @ParameterizedTest
    @MethodSource("hashTestArgProvider")
    void hash(
            String id1, String id2,
            String label1, String label2,
            String format1, String format2,
            int min1, int min2,
            int max1, int max2,
            Map<String, String> opts1, Map<String, String> opts2,
            boolean shouldBeEqual
    ) {
        CardActionInputField.Builder f1 = new CardActionInputField.Builder()
                .setId(id1)
                .setLabel(label1)
                .setFormat(format1)
                .setMinLength(min1)
                .setMaxLength(max1);
        opts1.forEach(f1::addOption);

        CardActionInputField.Builder f2 = new CardActionInputField.Builder()
                .setId(id2)
                .setLabel(label2)
                .setFormat(format2)
                .setMinLength(min2)
                .setMaxLength(max2);
        opts2.forEach(f2::addOption);

        if (shouldBeEqual) {
            assertEquals(f1.build().hash(), f2.build().hash());
        } else {
            assertNotEquals(f1.build().hash(), f2.build().hash());
        }
    }

    private static Stream<Arguments> hashTestArgProvider() {
        return Stream.of(
                Arguments.of("id1", "id1", "lbl1", "lbl1", "f1", "f1", 1, 1, 2, 2, Map.of(), Map.of(), true),
                Arguments.of("id1", "id1", "lbl1", "lbl1", "f1", "f1", 1, 1, 2, 2, Map.of("a", "1"), Map.of("a", "1"), true),
                Arguments.of("id1", "id1", "lbl1", "lbl1", "f1", "f1", 1, 1, 2, 2, Map.of("a", "1", "b", "2"), Map.of("a", "1", "b", "2"), true),
                Arguments.of("id1", "id1", "lbl1", "lbl1", "f1", "f1", 1, 1, 2, 2, Map.of("a", "", "b", ""), Map.of("a", "", "b", ""), true),
                // simple differences
                Arguments.of("X", "id1", "lbl1", "lbl1", "f1", "f1", 1, 1, 2, 2, Map.of("a", "1", "b", "2"), Map.of("a", "1", "b", "2"), false),
                Arguments.of("id1", "id1", "X", "lbl1", "f1", "f1", 1, 1, 2, 2, Map.of("a", "1", "b", "2"), Map.of("a", "1", "b", "2"), false),
                Arguments.of("id1", "id1", "lbl1", "lbl1", "X", "f1", 1, 1, 2, 2, Map.of("a", "1", "b", "2"), Map.of("a", "1", "b", "2"), false),
                Arguments.of("id1", "id1", "lbl1", "lbl1", "f1", "f1", -1, 1, 2, 2, Map.of("a", "1", "b", "2"), Map.of("a", "1", "b", "2"), false),
                Arguments.of("id1", "id1", "lbl1", "lbl1", "f1", "f1", 1, 1, -2, 2, Map.of("a", "1", "b", "2"), Map.of("a", "1", "b", "2"), false),
                Arguments.of("id1", "id1", "lbl1", "lbl1", "f1", "f1", 1, 1, 2, 2, Map.of("a", "1"), Map.of("a", "1", "b", "2"), false),
                Arguments.of("id1", "id1", "lbl1", "lbl1", "f1", "f1", 1, 1, 2, 2, Map.of("a", "X", "b", "2"), Map.of("a", "1", "b", "2"), false),
                Arguments.of("id1", "id1", "lbl1", "lbl1", "f1", "f1", 1, 1, 2, 2, Map.of("a", "1", "b", "X"), Map.of("a", "1", "b", "2"), false),
                // tricky differences
                // make sure it includes the key and value in the map
                Arguments.of("id1", "id1", "lbl1", "lbl1", "f1", "f1", 1, 1, 2, 2, Map.of("a", "1", "c", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of("id1", "id1", "lbl1", "lbl1", "f1", "f1", 1, 1, 2, 2, Map.of("a", "", "c", ""), Map.of("a", "", "b", ""), false),
                // make sure it doesn't fully rely on Map's toString
                Arguments.of("id1", "id1", "lbl1", "lbl1", "f1", "f1", 1, 1, 2, 2, Map.of("a", "1, b=2"), Map.of("a", "1", "b", "2"), false)
        );
    }

}
