/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static com.vmware.connectors.common.payloads.response.CardActionKey.DIRECT;
import static com.vmware.connectors.common.payloads.response.CardActionKey.USER_INPUT;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

class CardActionTest {

    @ParameterizedTest
    @MethodSource("hashTestArgProvider")
    void hash(
            UUID id1, UUID id2,
            boolean p1, boolean p2,
            CardActionKey ak1, CardActionKey ak2,
            HttpMethod t1, HttpMethod t2,
            String u1, String u2,
            boolean ar1, boolean ar2,
            String m1, String m2,
            boolean rc1, boolean rc2,
            String lbl1, String lbl2,
            String cl1, String cl2,
            List<String> uif1, List<String> uif2,
            Map<String, String> rp1, Map<String, String> rp2,
            boolean shouldBeEqual
    ) {
        CardAction.Builder a1 = new CardAction.Builder()
                .setId(id1)
                .setPrimary(p1)
                .setActionKey(ak1)
                .setType(t1)
                .setUrl(u1)
                .setMutuallyExclusiveSetId(m1)
                .setLabel(lbl1)
                .setCompletedLabel(cl1)
                .setAllowRepeated(ar1)
                .setRemoveCardOnCompletion(rc1);
        uif1.forEach(uif -> a1.addUserInputField(new CardActionInputField.Builder().setId(uif).build()));
        rp1.forEach(a1::addRequestParam);

        CardAction.Builder a2 = new CardAction.Builder()
                .setId(id2)
                .setPrimary(p2)
                .setActionKey(ak2)
                .setType(t2)
                .setUrl(u2)
                .setMutuallyExclusiveSetId(m2)
                .setLabel(lbl2)
                .setCompletedLabel(cl2)
                .setAllowRepeated(ar2)
                .setRemoveCardOnCompletion(rc2);
        uif2.forEach(uif -> a2.addUserInputField(new CardActionInputField.Builder().setId(uif).build()));
        rp2.forEach(a2::addRequestParam);

        if (shouldBeEqual) {
            assertEquals(a1.build().hash(), a2.build().hash());
        } else {
            assertNotEquals(a1.build().hash(), a2.build().hash());
        }
    }

    private static Stream<Arguments> hashTestArgProvider() {
        var id1 = UUID.randomUUID();
        var x = UUID.randomUUID();
        return Stream.of(
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of(), List.of(), Map.of(), Map.of(), true),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of(), List.of(), Map.of(), Map.of(), true),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("", ""), List.of("", ""), Map.of("a", "", "b", ""), Map.of("a", "", "b", ""), true),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "b", "2"), Map.of("a", "1", "b", "2"), true),
                // the id (uuid) doesn't affect the hash
                Arguments.of(id1, x, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "b", "2"), Map.of("a", "1", "b", "2"), true),

                // simple differences
                Arguments.of(id1, id1, false, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, USER_INPUT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, PUT, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "X", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", false, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "X", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", false, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "X", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "X", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("X", "b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "X", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "b", "X"), Map.of("a", "1", "b", "1"), false),

                // tricky List differences
                // make sure it doesn't fully rely on List's toString
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a,b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a, b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                // make sure it doesn't fully rely on separators
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a|b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a | b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a;b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a; b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a b"), List.of("a", "b"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                // make sure it doesn't rely on space separator and trimming the input
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a  b", "", "c"), List.of("a", "", "b  c"), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),
                // make sure it handles different size empty content
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("", ""), List.of("", "", ""), Map.of("a", "1", "b", "1"), Map.of("a", "1", "b", "1"), false),

                // tricky Map differences
                // make sure it includes the key and value in the map
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1", "c", "1"), Map.of("a", "1", "b", "1"), false),
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "", "c", ""), Map.of("a", "", "b", ""), false),
                // make sure it doesn't fully rely on Map's toString
                Arguments.of(id1, id1, true, true, DIRECT, DIRECT, POST, POST, "u1", "u1", true, true, "m1", "m1", true, true, "lbl1", "lbl1", "cl1", "cl1",
                        List.of("a", "b"), List.of("a", "b"), Map.of("a", "1, b=2"), Map.of("a", "1", "b", "2"), false)
        );
    }

}
