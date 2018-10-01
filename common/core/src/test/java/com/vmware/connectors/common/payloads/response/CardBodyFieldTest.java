/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.vmware.connectors.common.payloads.response.CardBodyFieldType.COMMENT;
import static com.vmware.connectors.common.payloads.response.CardBodyFieldType.GENERAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CardBodyFieldTest {

    @ParameterizedTest
    @MethodSource("hashTestArgProvider")
    void hash(
            CardBodyFieldType type1, CardBodyFieldType type2,
            String title1, String title2,
            String desc1, String desc2,
            List<Map<String, String>> c1, List<Map<String, String>> c2,
            boolean shouldBeEqual
    ) {
        CardBodyField.Builder f1 = new CardBodyField.Builder()
                .setType(type1)
                .setTitle(title1)
                .setDescription(desc1);
        c1.forEach(f1::addContent);

        CardBodyField.Builder f2 = new CardBodyField.Builder()
                .setType(type2)
                .setTitle(title2)
                .setDescription(desc2);
        c2.forEach(f2::addContent);

        if (shouldBeEqual) {
            assertEquals(f1.build().hash(), f2.build().hash());
        } else {
            assertNotEquals(f1.build().hash(), f2.build().hash());
        }
    }

    private static Stream<Arguments> hashTestArgProvider() {
        return Stream.of(
                Arguments.of(GENERAL, GENERAL, "t1", "t1", "d1", "d1", List.of(), List.of(), true),
                Arguments.of(GENERAL, GENERAL, "t1", "t1", "d1", "d1", List.of(Map.of()), List.of(Map.of()), true),
                Arguments.of(GENERAL, GENERAL, "t1", "t1", "d1", "d1", List.of(Map.of("a", "1", "b", "2")), List.of(Map.of("a", "1", "b", "2")), true),
                Arguments.of(GENERAL, GENERAL, "t1", "t1", "d1", "d1", List.of(Map.of("a", "1", "b", "")), List.of(Map.of("a", "1", "b", "")), true),
                // simple differences
                Arguments.of(COMMENT, GENERAL, "t1", "t1", "d1", "d1", List.of(Map.of("a", "1", "b", "")), List.of(Map.of("a", "1", "b", "")), false),
                Arguments.of(GENERAL, GENERAL, "tX", "t1", "d1", "d1", List.of(Map.of("a", "1", "b", "2")), List.of(Map.of("a", "1", "b", "2")), false),
                Arguments.of(GENERAL, GENERAL, "t1", "t1", "dX", "d1", List.of(Map.of("a", "1", "b", "2")), List.of(Map.of("a", "1", "b", "2")), false),
                Arguments.of(GENERAL, GENERAL, "t1", "t1", "d1", "d1", List.of(Map.of("a", "1")), List.of(Map.of("a", "1", "b", "2")), false),
                Arguments.of(GENERAL, GENERAL, "t1", "t1", "d1", "d1", List.of(Map.of("a", "X", "b", "2")), List.of(Map.of("a", "1", "b", "2")), false),
                Arguments.of(GENERAL, GENERAL, "t1", "t1", "d1", "d1", List.of(Map.of("a", "1", "b", "X")), List.of(Map.of("a", "1", "b", "2")), false),
                // tricky differences
                // make sure it includes the key and value in the map
                Arguments.of(GENERAL, GENERAL, "t1", "t1", "d1", "d1", List.of(Map.of("a", "1", "c", "1")), List.of(Map.of("a", "1", "b", "1")), false),
                // make sure it doesn't fully rely on Map's toString
                Arguments.of(GENERAL, GENERAL, "t1", "t1", "d1", "d1", List.of(Map.of("a", "1, b=2")), List.of(Map.of("a", "1", "b", "2")), false)
        );
    }

}
