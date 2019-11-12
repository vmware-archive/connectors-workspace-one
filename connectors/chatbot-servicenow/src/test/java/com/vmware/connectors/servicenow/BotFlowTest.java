/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.vmware.connectors.test.ControllerTestsBase;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.stream.Collectors;

import static com.vmware.connectors.utils.IgnoredFieldsReplacer.*;
import static com.vmware.connectors.utils.IgnoredFieldsReplacer.DUMMY_UUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

class BotFlowTest extends ControllerTestsBase {

    private static final String SNOW_AUTH_TOKEN = "test-GOOD-auth-token";

    private static final String OBJ_TYPE_CATALOG_ITEM = "catalog";
    private static final String OBJ_TYPE_TASK = "task";
    private static final String OBJ_TYPE_CART = "cart";

    private static final String OBJ_TYPE_BOT_DISCOVERY = "botDiscovery";

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/catalog-items",
            "/api/v1/tasks",
            "/api/v1/cart"})
    void testProtectedObjects(String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @ParameterizedTest
    @CsvSource({
            "PUT, /api/v1/cart",
            "DELETE, /api/v1/cart",
            "POST, /api/v1/task/create",
            "POST, /api/v1/checkout"})
    void testProtectedActions(String httpMethod, String uri) throws Exception {
        testProtectedResource(HttpMethod.valueOf(httpMethod), uri);
    }

    @Test
    void testDiscovery() throws Exception {
        String xForwardedHost = "https://my-connector";
        // Confirm connector has updated the host placeholder.
        String expectedMetadata = fromFile("/static/discovery/metadata.json")
                .replace("${CONNECTOR_HOST}", xForwardedHost);

        // Discovery metadata.json is at the connector root.
        webClient.get()
                .uri("/")
                .headers(ControllerTestsBase::headers)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .json(expectedMetadata)
                // Verify object type is 'botDiscovery'.
                .jsonPath("$.object_types.botDiscovery").exists();
    }

    @Test
    @Disabled
    void testCatalogItemsObject() throws Exception {

        // Find out the id of the requested Catalog.
        mockBackend.expect(requestTo("/api/sn_sc/servicecatalog/catalogs"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/catalogs.json"), APPLICATION_JSON));

        // Find out the id of the category requested.
        String catalogId = "e0d08b13c3330100c8b837659bba8fb4";
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/catalogs/{catalogId}", catalogId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/service_catalog.json"), APPLICATION_JSON));

        // Find out available items of type 'laptop' within 'hardware' category.
        String searchText = "laptop";
        String categoryId = "d258b953c611227a0146101fb1be7c31";
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/items" +
                        "?sysparm_text={searchText}" +
                        "&sysparm_category={categoryId}" +
                        "&sysparm_limit=10&sysparm_offset=0",
                searchText, categoryId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/laptop_items.json"), APPLICATION_JSON));

        // Make the object request and confirm.
        String body = requestObjects("/api/v1/catalog-items", SNOW_AUTH_TOKEN, "/botflows/connector/request/laptops.json",
                OBJ_TYPE_CATALOG_ITEM, null)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(BotFlowTest::normalizeBotObjects)
                .map(json -> json.replaceAll(mockBackend.url("/"), "https://mock-snow.com/"))
                .block();

        assertThat(body, sameJSONAs(fromFile("/botflows/connector/response/laptop_items.json")).allowingAnyArrayOrdering());
    }

    @Test
    @Disabled
    void testCatalogItemsObjError() throws Exception {

        mockBackend.expect(requestTo("/api/sn_sc/servicecatalog/catalogs"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/catalogs.json"), APPLICATION_JSON));

        String catalogId = "e0d08b13c3330100c8b837659bba8fb4";
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/catalogs/{catalogId}", catalogId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/service_catalog.json"), APPLICATION_JSON));


        requestObjects("/api/v1/catalog-items", SNOW_AUTH_TOKEN, "/botflows/connector/request/fruits.json",
                OBJ_TYPE_CATALOG_ITEM, null)
                .expectStatus().isBadRequest();
    }

    @Test
    void testViewMyTasksAction() throws Exception {
        String userEmailId = "admin@acme.com";
        mockBackend.expect(requestToUriTemplate(
                "/api/now/table/task?sysparm_display_value=true&sysparm_limit=5&sysparm_offset=0" +
                        "&opened_by.email={userEmailId}&active=true&sysparm_query=ORDERBYDESCsys_created_on",
                userEmailId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/task_ticket.json"), APPLICATION_JSON));

        String body = performAction(POST, "/api/v1/tasks", SNOW_AUTH_TOKEN, new LinkedMultiValueMap<>())
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(res -> normalizeBotObjects(res, mockBackend.url("/")))
                .block();

        assertThat(body, sameJSONAs(fromFile("/botflows/connector/response/task_ticket.json")).allowingAnyArrayOrdering());
    }


    @Test
    void testViewMyTasksActionIftasksAreEmpty() throws Exception {
        String userEmailId = "admin@acme.com";
        mockBackend.expect(requestToUriTemplate(
                "/api/now/table/task?sysparm_display_value=true&sysparm_limit=5&sysparm_offset=0" +
                        "&opened_by.email={userEmailId}&active=true&sysparm_query=ORDERBYDESCsys_created_on",
                userEmailId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/task_ticket_if_empty.json"), APPLICATION_JSON));

        String body = performAction(POST, "/api/v1/tasks", SNOW_AUTH_TOKEN, new LinkedMultiValueMap<>())
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(res -> normalizeBotObjects(res, mockBackend.url("/")))
                .block();

        assertThat(body, sameJSONAs(fromFile("/botflows/servicenow/response/task_ticket_if_empty_connector_response.json")).allowingAnyArrayOrdering());
    }

    @Test
    void testViewMyTasksActionShouldReturnServiceNowUrlWhenMoreThanFiveOpenTickets() throws Exception {
        String userEmailId = "admin@acme.com";
        mockBackend.expect(requestToUriTemplate(
                "/api/now/table/task?sysparm_display_value=true&sysparm_limit=5&sysparm_offset=0" +
                        "&opened_by.email={userEmailId}&active=true&sysparm_query=ORDERBYDESCsys_created_on",
                userEmailId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/task_ticket_more_than_5.json"), APPLICATION_JSON));

        String body = performAction(POST, "/api/v1/tasks", SNOW_AUTH_TOKEN, new LinkedMultiValueMap<>())
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(res -> normalizeBotObjects(res, mockBackend.url("/")))
                .block();

        assertThat(body, sameJSONAs(fromFile("/botflows/connector/response/task_ticket_more_than_5.json")).allowingAnyArrayOrdering());
    }

    @ParameterizedTest
    @CsvSource({
            " , /botflows/connector/response/cart.json",
            "xx, /botflows/connector/response/cart_xx.json"})
    @Disabled
    void testCartObject(String language, String expectedCartFileName) throws Exception {
        expectCartRequest();

        String body = requestObjects("/api/v1/cart", SNOW_AUTH_TOKEN, "/botflows/connector/request/cart.json",
                OBJ_TYPE_CART, language)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(BotFlowTest::normalizeBotObjects)
                .map(json -> json.replaceAll(mockBackend.url("/"), "https://mock-snow.com/"))
                .block();

        assertThat(body, sameJSONAs(fromFile(expectedCartFileName)).allowingAnyArrayOrdering());
    }

    @Test
    void testBotDiscoveryObject() throws Exception {

        String body = requestObjects("/bot-discovery", SNOW_AUTH_TOKEN,
                "/botflows/connector/request/bot_discovery_object.json",
                OBJ_TYPE_BOT_DISCOVERY, null)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(BotFlowTest::normalizeBotObjects)
                .block();

        assertThat(body, sameJSONAs(fromFile("/botflows/connector/response/bot_discovery_object.json")).allowingAnyArrayOrdering());

        Object botDiscoveryObj = JsonPath.parse(body).read("$.objects[0]");
        String botDiscoveryJsonString = JsonPath.parse(botDiscoveryObj).jsonString();

        assertThat(botDiscoveryJsonString, 	matchesJsonSchema(fromFile("/bot-schema.json")));

    }

    @Test
    @Disabled  // ToDo - APF-2570. Enable the test.
    void testInvalidAdminConfig() throws Exception {

        requestObjects("/bot-discovery", SNOW_AUTH_TOKEN,
                "/botflows/connector/request/with_invalid_config.json",
                OBJ_TYPE_BOT_DISCOVERY, null)
                .expectStatus().isBadRequest()
                .expectBody().json(fromFile("/botflows/connector/response/with_invalid_config.json"));
    }

    @Test
    void testCreateTaskAction() throws IOException {
        String taskType = "ticket";
        mockBackend.expect(requestToUriTemplate("/api/now/table/{taskType}", taskType))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(content().json(fromFile("/botflows/servicenow/request/create_ticket.json")))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/create_ticket.json"), APPLICATION_JSON));

        // For creating task object.
        String taskNumber = "TKT0010006";

        expectTaskReqByNumber(taskType, taskNumber, "/botflows/servicenow/response/ticket_mouse_not_working.json");

        MultiValueMap<String, String> actionFormData = new LinkedMultiValueMap<>();
        actionFormData.set("type", taskType);
        actionFormData.set("shortDescription", "My mouse is not working.");

        String body = performAction(POST, "/api/v1/task/create", SNOW_AUTH_TOKEN, actionFormData)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(res -> normalizeBotObjects(res, mockBackend.url("/")))
                .block();

        assertThat(body, sameJSONAs(fromFile("/botflows/connector/response/create_ticket.json")).allowingAnyArrayOrdering());
    }

    @Test
    @Disabled
    void testAddCart() throws IOException {
        String itemId = "2ab7077237153000158bbfc8bcbe5da9"; //Macbook pro.
        Integer itemCount = 1;
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/items/{itemId}/add_to_cart", itemId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(content().json(String.format(
                        "{" +
                                "\"sysparm_quantity\": %d" + "}", itemCount)))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/add_mac_to_cart.json"), APPLICATION_JSON));

        // To deliver cart object
        expectCartRequest();


        MultiValueMap<String, String> actionFormData = new LinkedMultiValueMap<>();
        actionFormData.set("itemId", itemId);
        actionFormData.set("itemCount", String.valueOf(itemCount));

        String body = performAction(PUT, "/api/v1/cart", SNOW_AUTH_TOKEN, actionFormData)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(BotFlowTest::normalizeBotObjects)
                .map(json -> json.replaceAll(mockBackend.url("/"), "https://mock-snow.com/"))
                .block();

        assertThat(body, sameJSONAs(fromFile("/botflows/connector/response/add_mac_to_cart.json")).allowingAnyArrayOrdering());
    }

    @Test
    @Disabled
    void testDeleteFromCart() throws IOException {
        // Assume there is a mouse in the cart, initially.
        String cartItemId = "88faa613db113300ea92eb41ca961950"; //Mouse cart item id.
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/cart/{cartItemId}", cartItemId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(DELETE))
                .andRespond(withStatus(NO_CONTENT));

        // To deliver cart object
        expectCartRequest();

        String body = performAction(DELETE, "/api/v1/cart/" + cartItemId, SNOW_AUTH_TOKEN, null)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(BotFlowTest::normalizeBotObjects)
                .map(json -> json.replaceAll(mockBackend.url("/"), "https://mock-snow.com/"))
                .block();

        assertThat(body, sameJSONAs(fromFile("/botflows/connector/response/delete_mouse_from_cart.json")).allowingAnyArrayOrdering());
    }

    @Test
    @Disabled
    void testClearCart() throws IOException {
        expectCartRequest();

        String cartId = "6a27ad02db113300ea92eb41ca961933";
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/cart/{cart_id}/empty", cartId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(DELETE))
                .andRespond(withStatus(NO_CONTENT));

        performAction(DELETE, "/api/v1/cart", SNOW_AUTH_TOKEN, null)
                .expectStatus().isNoContent();
    }

    @Test
    @Disabled
    void testCheckout() throws IOException {
        mockBackend.expect(requestTo("/api/sn_sc/servicecatalog/cart/checkout"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(POST))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/checkout.json"), APPLICATION_JSON));

        String reqNumber = "REQ0010033"; // Checkout request ticket number.
        // To deliver task object.
        expectTaskReqByNumber("task", reqNumber, "/botflows/servicenow/response/ticket_checkout_request.json");

        String body = performAction(POST, "/api/v1/checkout", SNOW_AUTH_TOKEN, null)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(BotFlowTest::normalizeBotObjects)
                .block();

        assertThat(body, sameJSONAs(fromFile("/botflows/connector/response/checkout.json")).allowingAnyArrayOrdering());
    }

    private void expectTaskReqByNumber(String taskType, String taskNumber, String sNowResponseFile) throws IOException {
        mockBackend.expect(requestToUriTemplate("/api/now/table/{taskType}?sysparm_display_value=true&number={taskNumber}",
                taskType, taskNumber))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile(sNowResponseFile), APPLICATION_JSON));
    }

    private void expectCartRequest() throws IOException {
        mockBackend.expect(requestTo("/api/sn_sc/servicecatalog/cart"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/cart.json"), APPLICATION_JSON));
    }

    private WebTestClient.ResponseSpec performAction(HttpMethod method, String actionPath,
                                                     String sNowAuthToken, MultiValueMap<String, String> formData) {
        WebTestClient.RequestBodySpec requestSpec = webClient.method(method)
                .uri(actionPath)
                .accept(APPLICATION_JSON)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(X_AUTH_HEADER, "Bearer " + sNowAuthToken)
                .header("x-routing-template", "https://mf/connectors/abc123/INSERT_OBJECT_TYPE/")
                .headers(headers -> headers(headers, actionPath));

        if (formData != null) {
            requestSpec.contentType(APPLICATION_FORM_URLENCODED)
                    .syncBody(formData);
        }

        return requestSpec.exchange();
    }

    private WebTestClient.ResponseSpec requestObjects(String objReqPath, String sNowAuthToken, String requestFile,
                                                      String objectType, String language) throws Exception {
        return doPost(
                objReqPath,
                APPLICATION_JSON,
                sNowAuthToken,
                requestFile,
                objectType,
                language
        );
    }

    private WebTestClient.ResponseSpec doPost(
            String path,
            MediaType contentType,
            String authToken,
            String requestFile,
            String objectType,
            String language
    ) throws Exception {

        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(path)
                .contentType(contentType)
                .accept(APPLICATION_JSON)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(X_AUTH_HEADER, "Bearer " + authToken)
                .header("x-routing-prefix", String.format("https://mf/connectors/abc123/%s/", objectType))
                .headers(headers -> headers(headers, path))
                .syncBody(fromFile(requestFile));

        if (StringUtils.isNotBlank(language)) {
            spec = spec.header(ACCEPT_LANGUAGE, language);
        }

        return spec.exchange();
    }

    private static String normalizeBotObjects(String body) {
        Configuration configuration = Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .build();

        DocumentContext context = JsonPath.using(configuration).parse(body);
        context.set("$.objects[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID);
        // Above line can be removed, when all the bot flows move to the latest schema.
        context.set("$.objects[*].itemDetails[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID);
        context.set("$.objects[*].children[*].itemDetails[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID);
        return context.jsonString();
    }

    private static String normalizeBotObjects(String body, String backendBaseUrl) {
        return normalizeBotObjects(body.replace(backendBaseUrl, "https://dev15329.service-now.com/"));
    }
}

