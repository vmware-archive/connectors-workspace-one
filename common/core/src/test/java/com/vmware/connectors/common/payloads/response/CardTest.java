/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardAction;
import com.vmware.connectors.common.payloads.response.CardActionInputField;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.CardBodyField;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class CardTest {

    private OffsetDateTime creationDate = OffsetDateTime.of(2017, 3, 3, 14, 28, 17, (int) MILLISECONDS.toNanos(656), ZoneOffset.ofHoursMinutes(5, 30));

    private OffsetDateTime expirationDate = OffsetDateTime.of(2017, 5, 9, 14, 28, 17, (int) MILLISECONDS.toNanos(656), ZoneOffset.ofHoursMinutes(5, 30));

    private UUID uuid = UUID.fromString("628a6d06-1925-404b-b241-ff21b273c4ab");

    private ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());


    @Test
    void testSimpleRoundTrip() throws Exception {
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String originalJson = fromFile("simple.json");
        Card card = stringToCard(originalJson);
        assertThat(card, is(not(nullValue())));
        String serializedCard = cardToString(card);
        assertThat(serializedCard, is(not(nullValue())));

        JSONAssert.assertEquals(serializedCard, originalJson, true);
    }

    @Test
    void testDifferentCards() throws Exception {
        String originalJson = fromFile("simple.json");
        String rtOriginal = cardToString(stringToCard(originalJson));

        String differentJson = fromFile("bodyWithFields.json");
        String rtDifferent = cardToString(stringToCard(differentJson));
        assertThat(differentJson, is(not(nullValue())));

        JSONAssert.assertNotEquals(rtOriginal, rtDifferent, true);

    }

    @Test
    void testCardBodyFieldBuilder() throws IOException, JSONException {
        CardBodyField.Builder builder = new CardBodyField.Builder();

        builder.setTitle("Title of the Card");
        builder.setType("BOYBAND");
        builder.setDescription("The ultimate boy-band hero card");
        builder.addContent(Collections.singletonMap("inspiration", "DaVinci's Notebook"));

        CardBodyField fieldFromBuilder = builder.build();
        String jsonFromBuilder = mapper.writeValueAsString(fieldFromBuilder);

        assertThat(jsonFromBuilder, containsString("BOYB"));

        CardBodyField fieldFromJson = mapper.readValue(fromFile("cardBodyFieldFromBuilder.json"), CardBodyField.class);
        String jsonFromJson = mapper.writeValueAsString(fieldFromJson);

        assertThat(jsonFromJson, containsString("Notebook"));

        JSONAssert.assertEquals(jsonFromBuilder, jsonFromJson, true);
    }

    @Test
    void testCardActionBuilder() throws IOException, JSONException {
        CardAction.Builder builder = new CardAction.Builder();
        builder.setId(uuid).setActionKey("ADD_MONKEY_ACTION")
                .setLabel("Add a Monkey")
                .setType(HttpMethod.PUT)
                .setUrl("/bucket/3213412")
                .addRequestParam("flings_poo", "true");

        CardActionInputField.Builder inputFieldBuilder = new CardActionInputField.Builder();

        // Populate all the fields
        inputFieldBuilder.setId("monkey_name")
                .setLabel("Monkey Name")
                .setFormat("email")
                .setMinLength(3)
                .setMaxLength(20);
        builder.addUserInputField(inputFieldBuilder.build());

        // Minimum length < 0 should be converted to 0
        inputFieldBuilder.setId("color")
                .setLabel("Color")
                .setMinLength(-3);
        builder.addUserInputField(inputFieldBuilder.build());

        // Max length < min length should be converted to 0
        inputFieldBuilder.setId("species")
                .setLabel("Species")
                .setMinLength(5)
                .setMaxLength(1);
        builder.addUserInputField(inputFieldBuilder.build());

        CardAction actionFromBuilder = builder.build();

        assertThat(actionFromBuilder.getUserInput().get(1).getMinLength(), is(0));
        assertThat(actionFromBuilder.getUserInput().get(2).getMinLength(), is(5));
        assertThat(actionFromBuilder.getUserInput().get(2).getMaxLength(), is(0));

        String jsonFromBuilder = mapper.writeValueAsString(actionFromBuilder);

        assertThat(jsonFromBuilder, containsString("3213"));

        CardAction actionFromJson = mapper.readValue(fromFile("cardActionFromBuilder.json"), CardAction.class);
        String jsonFromJson = mapper.writeValueAsString(actionFromJson);

        assertThat(jsonFromJson, containsString("_poo"));

        JSONAssert.assertEquals(jsonFromBuilder, jsonFromJson, true);
    }

    @Test
    void testCardBodyBuilder() throws IOException, JSONException {
        CardBody.Builder builder = new CardBody.Builder();

        builder.setDescription("A lump of coal");

        CardBodyField.Builder fieldBuilder = new CardBodyField.Builder();
        fieldBuilder.setDescription("Vitreous fracture");
        fieldBuilder.setType("ANTHRACITE");
        fieldBuilder.setTitle("Assay results");

        builder.addField(fieldBuilder.build());

        CardBody bodyFromBuilder = builder.build();
        String jsonFromBuilder = mapper.writeValueAsString(bodyFromBuilder);

        assertThat(jsonFromBuilder, containsString("coal"));

        CardBody bodyFromJson = mapper.readValue(fromFile("cardBodyFromBuilder.json"), CardBody.class);
        String jsonFromJson = mapper.writeValueAsString(bodyFromJson);

        JSONAssert.assertEquals(jsonFromBuilder, jsonFromJson, true);
    }

    @Test
    void testCardBuilder() throws IOException, JSONException {
        Card.Builder cardBuilder = new Card.Builder();
        CardBody.Builder bodyBuilder = new CardBody.Builder();
        CardBodyField.Builder fieldBuilder = new CardBodyField.Builder();
        CardAction.Builder actionBuilder = new CardAction.Builder();
        cardBuilder.setId(uuid);
        cardBuilder.setCreationDate(creationDate);
        cardBuilder.setExpirationDate(expirationDate);
        cardBuilder.setName("Soliloquy");
        cardBuilder.setTemplate("/connectors/salesforce/templates/tragedy.hbs");
        cardBuilder.setHeader("To be or not to be...", "... that is the question:");

        fieldBuilder.setType("GENERAL");
        fieldBuilder.setTitle("Fortune");
        fieldBuilder.setDescription("outrageous");
        bodyBuilder.addField(fieldBuilder.build());

        fieldBuilder.setType("GENERAL");
        fieldBuilder.setTitle("Troubles");
        fieldBuilder.setDescription("a sea");
        bodyBuilder.addField(fieldBuilder.build());

        bodyBuilder.setDescription("Which is nobler in the mind?");

        cardBuilder.setBody(bodyBuilder.build());
        actionBuilder.setId(uuid);
        actionBuilder.setActionKey("SUFFER");
        actionBuilder.setLabel("Suffer slings and arrows");
        actionBuilder.setType(HttpMethod.GET);
        cardBuilder.addAction(actionBuilder.build());
        cardBuilder.addTag("cats").addTag("fails");

        actionBuilder.setActionKey("TAKE_ARMS");
        actionBuilder.setId(uuid);
        actionBuilder.setLabel("Take arms");
        actionBuilder.setType(HttpMethod.DELETE);
        cardBuilder.addAction(actionBuilder.build());

        Card cardFromBuilder = cardBuilder.setHash("test-hash").build();


        String jsonFromBuilder = mapper.writeValueAsString(cardFromBuilder);

        Card cardFromJson = mapper.readValue(fromFile("cardFromBuilder.json"), Card.class);
        String jsonFromJson = mapper.writeValueAsString(cardFromJson);

        assertThat(jsonFromJson, containsString("slings"));

        JSONAssert.assertEquals(jsonFromBuilder, jsonFromJson, true);
    }

    @Test
    void testMinimalCardBuilder() throws IOException, JSONException {
        Card.Builder cardBuilder = new Card.Builder();

        cardBuilder.setBody("Darkness comes, shrouded in extra darkness, shoved in even more darkness. It's a darkness turducken.");

        cardBuilder.setId(uuid);

        cardBuilder.setCreationDate(creationDate);

        Card cardFromBuilder = cardBuilder.setHash("test-hash").build();

        String jsonFromBuilder = mapper.writeValueAsString(cardFromBuilder);

        assertThat(jsonFromBuilder, containsString("turducken"));

        Card cardFromJson = mapper.readValue(fromFile("minimal.json"), Card.class);

        String jsonFromJson = mapper.writeValueAsString(cardFromJson);

        assertThat(jsonFromJson, containsString("shrouded"));

        JSONAssert.assertEquals(jsonFromBuilder, jsonFromJson, true);
    }

    private Card stringToCard(String json) throws IOException {
        return mapper.readValue(json, Card.class);
    }

    private String cardToString(Card card) throws JsonProcessingException {
        return mapper.writeValueAsString(card);
    }

    public String fromFile(String fileName) {
        String result = "";

        try {
            result = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(fileName), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Test
    void hash() {
        var card = new Card.Builder()
                .setHeader("test-header")
                .setHash("test-hash")
                .build();
        // the connector author can specify their own hash
        assertEquals("test-hash", card.getHash());
    }

    @ParameterizedTest
    @MethodSource("hashTestArgProvider")
    void hash(
            String n1, String n2,
            String t1, String t2,
            String h1, String h2,
            String b1, String b2,
            String image1, String image2,
            Integer importance1, Integer importance2,
            UUID id1, UUID id2,
            OffsetDateTime cd1, OffsetDateTime cd2,
            OffsetDateTime ed1, OffsetDateTime ed2,
            List<String> a1, List<String> a2,
            List<String> tags1, List<String> tags2,
            boolean shouldBeEqual
    ) {
        var c1 = new Card.Builder()
                .setName(n1)
                .setTemplate(t1)
                .setHeader(h1)
                .setBody(b1)
                .setImageUrl(image1)
                .setImportance(importance1)
                .setId(id1)
                .setCreationDate(cd1)
                .setExpirationDate(ed1);
        a1.forEach(a -> c1.addAction(new CardAction.Builder().setLabel(a).build()));
        tags1.forEach(c1::addTag);

        var c2 = new Card.Builder()
                .setName(n2)
                .setTemplate(t2)
                .setHeader(h2)
                .setBody(b2)
                .setImageUrl(image2)
                .setImportance(importance2)
                .setId(id2)
                .setCreationDate(cd2)
                .setExpirationDate(ed2);
        a2.forEach(a -> c2.addAction(new CardAction.Builder().setLabel(a).build()));
        tags2.forEach(c2::addTag);

        if (shouldBeEqual) {
            assertEquals(c1.build().getHash(), c2.build().getHash());
        } else {
            assertNotEquals(c1.build().getHash(), c2.build().getHash());
        }
    }

    private static Stream<Arguments> hashTestArgProvider() {
        var id1 = UUID.randomUUID();
        var xid = UUID.randomUUID();
        var d1 = OffsetDateTime.now().minusDays(2);
        var d2 = OffsetDateTime.now().minusDays(1);
        var xd = OffsetDateTime.now();
        return Stream.of(
                // 1-5
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of(), List.of(), List.of(), List.of(), true),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), true),
                // the id (uuid) doesn't affect the hash
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        xid, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), true),
                // the creation date doesn't affect the hash
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, xd, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), true),
                // the expiration date doesn't affect the hash
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, xd, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), true),

                // 6-15, simple differences
                Arguments.of("X", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "X", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "X", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "X", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "X", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", -1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("X", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "X"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("X", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "X"), List.of("c", "d"), false),

                // tricky differences

                // 16-19, make sure it doesn't fully rely on List's toString
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a,b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a, b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c,d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c, d"), List.of("c", "d"), false),

                // 20-29, make sure it doesn't fully rely on separators
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a|b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a | b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c|d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c | d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a;b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a; b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c;d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c; d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a ", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c ", "d"), List.of("c", "d"), false),

                // 30-31, make sure it doesn't rely on space separator and trimming the input
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a  b", "", "c"), List.of("a", "", "b  c"), List.of("c", "d"), List.of("c", "d"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c  d", "", "e"), List.of("c", "", "d  e"), false),

                // 32, make sure it handles different size empty content
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("", ""), List.of("", "", ""), List.of("c", "d"), List.of("c", "d"), false)
        );
    }

}
