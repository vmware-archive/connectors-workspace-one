/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow;

import com.vmware.connectors.test.ControllerTestsBase;
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

class BotFlowTest extends ControllerTestsBase {

    private static final String SNOW_AUTH_TOKEN = "test-GOOD-auth-token";

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
    void testCatalogItemsObject() throws Exception {

        // Find out the id of the requested Catalog.
        mockBackend.expect(requestTo("/api/sn_sc/servicecatalog/catalogs"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/catalogs.json"), APPLICATION_JSON));

        // Find out the id of the category requested.
        String catalogId = "e0d08b13c3330100c8b837659bba8fb4";
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/catalogs/{catalogId}/categories", catalogId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/service_catalog_categories.json"), APPLICATION_JSON));

        // Find out available items of type 'laptop' within 'hardware' category.
        String type = "laptop";
        String categoryId = "d258b953c611227a0146101fb1be7c31";
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/items" +
                "?sysparm_text={type}" +
                        "&sysparm_category={categoryId}" +
                        "&sysparm_limit=10&sysparm_offset=0",
                type, categoryId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/laptop_items.json"), APPLICATION_JSON));

        // Make the object request and confirm.
        requestObjects("/api/v1/catalog-items", SNOW_AUTH_TOKEN, "/botflows/connector/request/laptops.json",null)
                .expectStatus().is2xxSuccessful()
                .expectBody().json(fromFile("/botflows/connector/response/laptop_items.json"));
    }

    @Test
    void testCatalogItemsObjError() throws Exception {

        mockBackend.expect(requestTo("/api/sn_sc/servicecatalog/catalogs"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/catalogs.json"), APPLICATION_JSON));

        String catalogId = "e0d08b13c3330100c8b837659bba8fb4";
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/catalogs/{catalogId}/categories", catalogId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/service_catalog_categories.json"), APPLICATION_JSON));


        requestObjects("/api/v1/catalog-items", SNOW_AUTH_TOKEN, "/botflows/connector/request/fruits.json",null)
                .expectStatus().isBadRequest();
    }

    // ToDo: APF-2225 Do more to test optional token fields.
    @Test
    void testTaskObject() throws Exception {
        String taskType = "ticket";
        String userEmailId = "admin@acme.com";
        mockBackend.expect(requestToUriTemplate("/api/now/table/{taskType}?sysparm_limit=10&sysparm_offset=0&opened_by={userEmailId}",
                    taskType, userEmailId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/task_ticket.json"), APPLICATION_JSON));

        requestObjects("/api/v1/tasks", SNOW_AUTH_TOKEN, "/botflows/connector/request/task_ticket.json",null)
                .expectStatus().is2xxSuccessful()
                .expectBody().json(fromFile("/botflows/connector/response/task_ticket.json"));
    }

    @Test
    void testCartObject() throws Exception {
        mockBackend.expect(requestTo("/api/sn_sc/servicecatalog/cart"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/cart.json"), APPLICATION_JSON));

        webClient.post()
                .uri("/api/v1/cart")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .accept(APPLICATION_JSON)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(X_AUTH_HEADER, "Bearer " + SNOW_AUTH_TOKEN)
                .header("x-routing-prefix", "https://hero/connectors/servicenow/")
                .headers(ControllerTestsBase::headers)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody().json(fromFile("/botflows/connector/response/cart.json"));
    }

    @Test
    void testCreateTask() throws IOException {
        mockBackend.expect(requestTo("/api/now/table/ticket"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(content().json(fromFile("/botflows/servicenow/request/create_ticket.json")))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/create_ticket.json"), APPLICATION_JSON));

        MultiValueMap<String, String> actionFormData = new LinkedMultiValueMap<>();
        actionFormData.set("type", "ticket");
        actionFormData.set("short_description", "My mouse is not working.");

        performAction(POST, "/api/v1/task/create", SNOW_AUTH_TOKEN, actionFormData)
        .expectStatus().is2xxSuccessful()
        .expectBody().json(fromFile("/botflows/connector/response/create_ticket.json"));
    }

    @Test
    void testAddCart() throws IOException {
        String itemId = "774906834fbb4200086eeed18110c737"; //Macbook pro.
        Integer itemCount = 1;
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/items/{itemId}/add_to_cart", itemId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(content().json(String.format(
                        "{" +
                                "\"sysparm_quantity\": %d" + "}", itemCount)))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/add_mac_to_cart.json"), APPLICATION_JSON));

        MultiValueMap<String, String> actionFormData = new LinkedMultiValueMap<>();
        actionFormData.set("item_id", itemId);
        actionFormData.set("item_count", String.valueOf(itemCount));

        performAction(PUT, "/api/v1/cart", SNOW_AUTH_TOKEN, actionFormData)
                .expectStatus().is2xxSuccessful()
                .expectBody().json(fromFile("/botflows/connector/response/add_mac_to_cart.json"));
    }

    @Test
    void testDeleteCart() {
        String cartItemId = "88faa613db113300ea92eb41ca961950"; //Macbook pro in cart.
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/cart/{cartItemId}", cartItemId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(DELETE))
                .andRespond(withStatus(NO_CONTENT));

        MultiValueMap<String, String> actionFormData = new LinkedMultiValueMap<>();
        actionFormData.set("entry_id", cartItemId);

        performAction(DELETE, "/api/v1/cart", SNOW_AUTH_TOKEN, actionFormData)
                .expectStatus().isNoContent();
    }

    @Test
    void testCheckout() throws IOException {
        mockBackend.expect(requestTo("/api/sn_sc/servicecatalog/cart/checkout"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(POST))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/checkout.json"), APPLICATION_JSON));

        performAction(POST, "/api/v1/checkout", SNOW_AUTH_TOKEN, null)
                .expectStatus().is2xxSuccessful()
                .expectBody().json(fromFile("/botflows/connector/response/checkout.json"));

    }

    private WebTestClient.ResponseSpec performAction(HttpMethod method, String actionPath,
                                                     String sNowAuthToken, MultiValueMap<String, String> formData) {
        WebTestClient.RequestBodySpec requestSpec = webClient.method(method)
                .uri(actionPath)
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .accept(APPLICATION_JSON)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(X_AUTH_HEADER, "Bearer " + sNowAuthToken)
                .header("x-routing-prefix", "https://hero/connectors/servicenow/")
                .headers(ControllerTestsBase::headers);

        if (formData != null) {
            requestSpec.contentType(APPLICATION_FORM_URLENCODED)
                    .syncBody(formData);
        }

        return requestSpec.exchange();
    }

    private WebTestClient.ResponseSpec requestObjects(String objReqPath, String sNowAuthToken, String requestFile,
                                                      String language) throws Exception {
        return doPost(
                objReqPath,
                APPLICATION_JSON,
                sNowAuthToken,
                requestFile,
                language
        );
    }

    private WebTestClient.ResponseSpec doPost(
            String path,
            MediaType contentType,
            String authToken,
            String requestFile,
            String language
    ) throws Exception {

        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(path)
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(contentType)
                .accept(APPLICATION_JSON)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(X_AUTH_HEADER, "Bearer " + authToken)
                .header("x-routing-prefix", "https://hero/connectors/servicenow/")
                .headers(ControllerTestsBase::headers)
                .syncBody(fromFile(requestFile));

        if (language != null) {
            spec = spec.header(ACCEPT_LANGUAGE, language);
        }

        return spec.exchange();
    }
}
