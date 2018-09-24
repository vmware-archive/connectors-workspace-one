/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common;

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
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


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

        Card cardFromBuilder = cardBuilder.build();


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

        Card cardFromBuilder = cardBuilder.build();

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
}