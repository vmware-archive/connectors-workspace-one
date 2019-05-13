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

import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
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

    //ToDo: Test connector actions.

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
