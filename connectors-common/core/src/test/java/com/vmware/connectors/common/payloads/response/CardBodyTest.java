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

import static org.junit.jupiter.api.Assertions.*;

class CardBodyTest {

    @ParameterizedTest
    @MethodSource("hashTestArgProvider")
    void hash(String d1, String d2, List<String> f1, List<String> f2, boolean shouldBeEqual) {
        CardBody.Builder b1 = new CardBody.Builder()
                .setDescription(d1);

        f1.forEach(f -> b1.addField(new CardBodyField.Builder().setTitle(f).build()));

        CardBody.Builder b2 = new CardBody.Builder()
                .setDescription(d2);

        f2.forEach(f -> b2.addField(new CardBodyField.Builder().setTitle(f).build()));

        if (shouldBeEqual) {
            assertEquals(b1.build().hash(), b2.build().hash());
        } else {
            assertNotEquals(b1.build().hash(), b2.build().hash());
        }
    }

    private static Stream<Arguments> hashTestArgProvider() {
        return Stream.of(
                Arguments.of("t", "t", List.of(), List.of(), true),
                Arguments.of("t", "t", List.of("a"), List.of("a"), true),
                Arguments.of("t", "t", List.of("a", "b"), List.of("a", "b"), true),
                Arguments.of("t", "t", List.of("a", "b", "c"), List.of("a", "b", "c"), true),
                Arguments.of("", "", List.of("a", "b", "c"), List.of("a", "b", "c"), true),
                Arguments.of("t", "t", List.of("a", "", "c"), List.of("a", "", "c"), true),
                Arguments.of("t", "t", List.of("", "", ""), List.of("", "", ""), true),
                // simple differences
                Arguments.of("t", "X", List.of("a", "b", "c"), List.of("a", "b", "c"), false),
                Arguments.of("t", "t", List.of("X", "b", "c"), List.of("a", "b", "c"), false),
                Arguments.of("t", "t", List.of("a", "X", "c"), List.of("a", "b", "c"), false),
                Arguments.of("t", "t", List.of("a", "b", "X"), List.of("a", "b", "c"), false),
                // make sure it handles different size empty content
                Arguments.of("t", "t", List.of("", ""), List.of("", "", ""), false)
        );
    }

}
