/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vmware.connectors.common.utils.JsonUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class CardTest {

    private OffsetDateTime creationDate = OffsetDateTime.of(2017, 3, 3, 14, 28, 17, (int) MILLISECONDS.toNanos(656), ZoneOffset.ofHoursMinutes(5, 30));

    private OffsetDateTime expirationDate = OffsetDateTime.of(2017, 5, 9, 14, 28, 17, (int) MILLISECONDS.toNanos(656), ZoneOffset.ofHoursMinutes(5, 30));

    private OffsetDateTime dueDate = OffsetDateTime.of(2020, 5, 15, 14, 28, 17, (int) MILLISECONDS.toNanos(599), ZoneOffset.ofHoursMinutes(5, 30));

    private UUID uuid = UUID.fromString("628a6d06-1925-404b-b241-ff21b273c4ab");

    private ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final URI ANOTHER_TEST_HREF =
            UriComponentsBuilder.fromUriString("https://impl.dummyserver1.com").build().toUri();
    private static final String ANOTHER_TEXT = "Open In dummy Server1";
    private static final OpenInLink OPEN_IN_LINK_1 = JsonUtils.convertFromJsonFile("open_in_link.json", OpenInLink.class);
    private static final OpenInLink OPEN_IN_LINK_2 = OpenInLink.builder()
            .href(ANOTHER_TEST_HREF)
            .text(ANOTHER_TEXT)
            .build();

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

        CardBodyFieldItem.Builder item = new CardBodyFieldItem.Builder();
        item.setType(CardBodyFieldType.GENERAL);
        item.setTitle("Expense Type");
        item.setDescription("Meals & Entertainment");

        builder.setTitle("Title of the Card");
        builder.setSubtitle("Subtitle of the Card");
        builder.setType("BOYBAND");
        builder.setDescription("The ultimate boy-band hero card");
        builder.addContent(Collections.singletonMap("inspiration", "DaVinci's Notebook"));
        builder.addItem(item.build());

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
   public void testCardLinks() throws Exception {
       mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
       String originalJson = fromFile("simple.json");
       Card card = stringToCard(originalJson);
       Assertions.assertThat(card.getLinks()).hasSize(1);
       Assertions.assertThat(card.getLinks().get(0).getHref()).isEqualTo(OPEN_IN_LINK_1.getHref());
       Assertions.assertThat(card.getLinks().get(0).getText()).isEqualTo(OPEN_IN_LINK_1.getText());
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
        cardBuilder.setDueDate(dueDate);
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

    @Test
    void testSticky() throws IOException {
        OffsetDateTime stickyDate = OffsetDateTime.of(2020, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);

        Card cardFromBuilder = new Card.Builder()
                .setBody("unit test card body")
                .setId(uuid)
                .setCreationDate(creationDate)
                .setSticky(new Sticky(
                        stickyDate,
                        "foo"
                ))
                .setHash("test-hash")
                .build();

        String jsonFromBuilder = mapper.writeValueAsString(cardFromBuilder);

        assertThat(jsonFromBuilder, containsString("\"body\""));
        assertThat(jsonFromBuilder, containsString("\"unit test card body\""));
        assertThat(jsonFromBuilder, containsString("\"sticky\""));
        assertThat(jsonFromBuilder, containsString("\"until\""));
        // TODO - The unit tests don't seem to be handling this correctly
//        assertThat(jsonFromBuilder, containsString("\"2020-12-31T23:59:59.000Z\""));
        assertThat(jsonFromBuilder, containsString("\"type\""));
        assertThat(jsonFromBuilder, containsString("\"foo\""));

        Card cardFromJson = mapper.readValue(fromFile("sticky.json"), Card.class);
        assertThat(cardFromJson.getSticky().getType(), is(equalTo("foobar")));
        assertThat(cardFromJson.getSticky().getUntil(), is(equalTo(stickyDate)));
    }

    @Test
    void testActionContentTypeJson() throws IOException {
        Card cardFromBuilder = new Card.Builder()
                .setBody("unit test card body")
                .setId(uuid)
                .setCreationDate(creationDate)
                .addAction(
                        new CardAction.Builder()
                                .setActionKey(CardActionKey.DIRECT)
                                .setLabel("foo")
                                .setType(HttpMethod.POST)
                                .setContentType(MediaType.APPLICATION_JSON)
                                .build()
                )
                .setHash("test-hash")
                .build();

        String jsonFromBuilder = mapper.writeValueAsString(cardFromBuilder);

        assertThat(jsonFromBuilder, containsString("\"body\""));
        assertThat(jsonFromBuilder, containsString("\"unit test card body\""));
        assertThat(jsonFromBuilder, containsString("\"actions\""));
        assertThat(jsonFromBuilder, containsString("\"content_type\""));
        assertThat(jsonFromBuilder, containsString("\"application/json\""));

        Card cardFromJson = mapper.readValue(fromFile("action-content-type.json"), Card.class);
        assertThat(cardFromJson.getActions().get(0).getContentType(), is(equalTo("application/json")));
    }

    @Test
    void testActionContentTypeCustom() throws IOException {
        Card cardFromBuilder = new Card.Builder()
                .setBody("unit test card body")
                .setId(uuid)
                .setCreationDate(creationDate)
                .addAction(
                        new CardAction.Builder()
                                .setActionKey(CardActionKey.DIRECT)
                                .setLabel("foo")
                                .setType(HttpMethod.POST)
                                .setContentType("foo/bar")
                                .build()
                )
                .setHash("test-hash")
                .build();

        String jsonFromBuilder = mapper.writeValueAsString(cardFromBuilder);

        assertThat(jsonFromBuilder, containsString("\"body\""));
        assertThat(jsonFromBuilder, containsString("\"unit test card body\""));
        assertThat(jsonFromBuilder, containsString("\"actions\""));
        assertThat(jsonFromBuilder, containsString("\"content_type\""));
        assertThat(jsonFromBuilder, containsString("\"foo/bar\""));
    }

    @Test
    void testActionUserInputDisplayContent() throws IOException {
        Card cardFromBuilder = new Card.Builder()
                .setBody("unit test card body")
                .setId(uuid)
                .setCreationDate(creationDate)
                .addAction(
                        new CardAction.Builder()
                                .setActionKey(CardActionKey.USER_INPUT)
                                .setLabel("foo")
                                .setType(HttpMethod.POST)
                                .addUserInputField(
                                        new CardActionInputField.Builder()
                                                .setId("foo")
                                                .setLabel("bar")
                                                .setDisplayContent("baz")
                                                .build()
                                )
                                .build()
                )
                .setHash("test-hash")
                .build();

        String jsonFromBuilder = mapper.writeValueAsString(cardFromBuilder);

        assertThat(jsonFromBuilder, containsString("\"body\""));
        assertThat(jsonFromBuilder, containsString("\"unit test card body\""));
        assertThat(jsonFromBuilder, containsString("\"actions\""));
        assertThat(jsonFromBuilder, containsString("\"user_input\""));
        assertThat(jsonFromBuilder, containsString("\"display_content\""));
        assertThat(jsonFromBuilder, containsString("\"baz\""));

        Card cardFromJson = mapper.readValue(fromFile("action-input-display-content.json"), Card.class);
        assertThat(cardFromJson.getActions().get(0).getUserInput().get(0).getDisplayContent(), is(equalTo("baz")));
    }

    @Test
    void testBannerJson() throws IOException {
        Card cardFromBuilder = new Card.Builder()
                .setBody("unit test card body")
                .setId(uuid)
                .setCreationDate(creationDate)
                .setBanner(new CardBanner(List.of(
                        new BannerItem(BannerItem.Type.VIDEO, "video-href", "video-title", "video-description"),
                        new BannerItem(BannerItem.Type.IMAGE, "image-href", null, "image-description")
                )))
                .setHash("test-hash")
                .build();

        String jsonFromBuilder = mapper.writeValueAsString(cardFromBuilder);

        assertThat(jsonFromBuilder, containsString("\"body\""));
        assertThat(jsonFromBuilder, containsString("\"unit test card body\""));
        assertThat(jsonFromBuilder, containsString("\"banner\""));
        assertThat(jsonFromBuilder, containsString("\"items\""));
        assertThat(jsonFromBuilder, containsString("\"type\""));
        assertThat(jsonFromBuilder, containsString("\"href\""));
        assertThat(jsonFromBuilder, containsString("\"title\""));
        assertThat(jsonFromBuilder, containsString("\"description\""));

        assertThat(jsonFromBuilder, containsString("\"video\""));
        assertThat(jsonFromBuilder, containsString("\"video-href\""));
        assertThat(jsonFromBuilder, containsString("\"video-title\""));
        assertThat(jsonFromBuilder, containsString("\"video-description\""));

        assertThat(jsonFromBuilder, containsString("\"image\""));
        assertThat(jsonFromBuilder, containsString("\"image-href\""));
        assertThat(jsonFromBuilder, not(containsString("null")));
        assertThat(jsonFromBuilder, containsString("\"image-description\""));

        Card cardFromJson = mapper.readValue(fromFile("banner.json"), Card.class);

        assertThat(cardFromJson.getBanner().getItems().get(0).getType(), is(equalTo(BannerItem.Type.VIDEO)));
        assertThat(cardFromJson.getBanner().getItems().get(0).getHref(), is(equalTo("https://www.abcd.com/video.stream")));
        assertThat(cardFromJson.getBanner().getItems().get(0).getDescription(), is(equalTo("This is a description for a video type banner item")));
        assertThat(cardFromJson.getBanner().getItems().get(0).getTitle(), is(equalTo("Streaming video")));

        assertThat(cardFromJson.getBanner().getItems().get(1).getType(), is(equalTo(BannerItem.Type.IMAGE)));
        assertThat(cardFromJson.getBanner().getItems().get(1).getHref(), is(equalTo("https://www.adsad.com/abc.png")));
        assertThat(cardFromJson.getBanner().getItems().get(1).getDescription(), is(equalTo("This is a description for an image type banner item")));
        assertThat(cardFromJson.getBanner().getItems().get(1).getTitle(), is(nullValue()));
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
            List<OpenInLink> links1, List<OpenInLink> links2,
            Sticky s1, Sticky s2,
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
                .setSticky(s1)
                .setCreationDate(cd1)
                .setExpirationDate(ed1);
        a1.forEach(a -> c1.addAction(new CardAction.Builder().setLabel(a).build()));
        links1.forEach(openInLink1 -> c1.addLinks(openInLink1));
        tags1.forEach(c1::addTag);

        var c2 = new Card.Builder()
                .setName(n2)
                .setTemplate(t2)
                .setHeader(h2)
                .setBody(b2)
                .setImageUrl(image2)
                .setImportance(importance2)
                .setId(id2)
                .setSticky(s2)
                .setCreationDate(cd2)
                .setExpirationDate(ed2);
        a2.forEach(a -> c2.addAction(new CardAction.Builder().setLabel(a).build()));
        links2.forEach(openInLink2 -> c2.addLinks(openInLink2));
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
                // 1-6
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of(), List.of(), List.of(), List.of(), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        new Sticky(d1, "s1"), new Sticky(d1, "s1"), true),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1, OPEN_IN_LINK_2), List.of(OPEN_IN_LINK_1, OPEN_IN_LINK_2),
                        new Sticky(d1, null), new Sticky(d1, null), true),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1, OPEN_IN_LINK_2), List.of(OPEN_IN_LINK_1, OPEN_IN_LINK_2),
                        null, null, true),
                // the id (uuid) doesn't affect the hash
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        xid, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_2), List.of(OPEN_IN_LINK_2),
                        null, null, true),
                // the creation date doesn't affect the hash
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, xd, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, true),
                // the expiration date doesn't affect the hash
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, xd, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, true),

                // 7-20, simple differences
                Arguments.of("X", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "X", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "X", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "X", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "X", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", -1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("X", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "X"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("X", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c", "X"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of(), List.of(), List.of(), List.of(), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        new Sticky(d1, "s1"), new Sticky(d1, "s2"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of(), List.of(), List.of(), List.of(), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        new Sticky(d1, "s1"), new Sticky(d2, "s1"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of(), List.of(), List.of(), List.of(), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, new Sticky(d1, "s1"), false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of(), List.of(), List.of(), List.of(), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        new Sticky(d1, "s1"), null, false),

                // tricky differences

                // 21-24, make sure it doesn't fully rely on List's toString
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a,b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a, b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c,d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c, d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),

                // 25-34, make sure it doesn't fully rely on separators
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a|b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a | b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c|d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c | d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a;b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a; b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c;d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c; d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a ", "b"), List.of("a", "b"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c ", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),

                // 35-36, make sure it doesn't rely on space separator and trimming the input
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a  b", "", "c"), List.of("a", "", "b  c"), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("a", "b"), List.of("a", "b"), List.of("c  d", "", "e"), List.of("c", "", "d  e"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),

                // 37, make sure it handles different size empty content
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of("", ""), List.of("", "", ""), List.of("c", "d"), List.of("c", "d"), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, false),

                // 38-42, test arguments for openInlink
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of(), List.of(), List.of(), List.of(), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_1),
                        null, null, true),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                        null, null, true),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of(), List.of(), List.of(), List.of(), List.of(OPEN_IN_LINK_1), List.of(OPEN_IN_LINK_2),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of(), List.of(), List.of(), List.of(), List.of(OPEN_IN_LINK_1, OPEN_IN_LINK_2), List.of(OPEN_IN_LINK_1),
                        null, null, false),
                Arguments.of("n1", "n1", "t1", "t1", "h1", "h1", "b1", "b1", "image1", "image1", 1, 1,
                        id1, id1, d1, d1, d2, d2, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(OPEN_IN_LINK_1),
                        null, null, false)
        );
    }

}
