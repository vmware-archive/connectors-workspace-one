/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.utils.ArgumentsStreamBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vmware.connectors.utils.IgnoredFieldsReplacer.DUMMY_UUID;
import static com.vmware.connectors.utils.IgnoredFieldsReplacer.UUID_PATTERN;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class BotFlowTest extends ControllerTestsBase {

    private static final String SNOW_AUTH_TOKEN = "test-GOOD-auth-token";
    private static final String OBJ_TYPE_BOT_DISCOVERY = "botDiscovery";
    private static final String CART_ID = "6a27ad02db113300ea92eb41ca961933";
    private static final String CLEAR_CART_URL = "/api/sn_sc/servicecatalog/cart/{cart_id}/empty";
    private static final String BEARER = "Bearer ";
    private static final String CART_URL_CONNECTOR = "/api/v1/cart";
    private static final String CLEAR_CART_RESP_FILE = "/botflows/connector/response/clear_cart.json";
    private static final String CLEAR_CART_ERR_FILE = "/botflows/servicenow/response/clear_cart_error.json";
    private static final String CLEAR_CART_CONNECTOR_RESP = "/botflows/connector/response/clear_cart_error.json";
    public static final String BOTFLOWS_SERVICENOW_RESPONSE_LAPTOP_ITEMS_JSON = "/botflows/servicenow/response/laptop_items.json";
    public static final String BOTFLOWS_CONNECTOR_RESPONSE_LAPTOP_ITEMS_JSON = "/botflows/connector/response/laptop_items.json";
    public static final String BOTFLOWS_SERVICENOW_RESPONSE_LAPTOP_ITEMS_EMPTY_JSON = "/botflows/servicenow/response/laptop_items_empty.json";
    public static final String BOTFLOWS_CONNECTOR_RESPONSE_LAPTOP_ITEMS_EMPTY_JSON = "/botflows/connector/response/laptop_items_empty.json";
    @Autowired ServerProperties serverProperties;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/catalog-items",
            "/api/v1/tasks",
            "/api/v1/cart"})
    void testProtectedObjects(String uri) throws Exception {
        final String appContextPath = serverProperties.getServlet().getContextPath();
        testProtectedResource(POST, appContextPath + uri);
    }

    private static Stream<Arguments> inputsForGetCategoryItems() {
        return new ArgumentsStreamBuilder()
                .add(BOTFLOWS_SERVICENOW_RESPONSE_LAPTOP_ITEMS_JSON, BOTFLOWS_CONNECTOR_RESPONSE_LAPTOP_ITEMS_JSON)
                .add(BOTFLOWS_SERVICENOW_RESPONSE_LAPTOP_ITEMS_EMPTY_JSON, BOTFLOWS_CONNECTOR_RESPONSE_LAPTOP_ITEMS_EMPTY_JSON)
                .build();
    }

    @ParameterizedTest
    @CsvSource({
            "PUT, /api/v1/cart",
            "DELETE, /api/v1/cart",
            "POST, /api/v1/task/create",
            "POST, /api/v1/checkout"})
    void testProtectedActions(String httpMethod, String uri) throws Exception {
        final String appContextPath = serverProperties.getServlet().getContextPath();
        testProtectedResource(HttpMethod.valueOf(httpMethod), appContextPath + uri);
    }

    @Test void testDiscovery() throws Exception {
        final String appContextPath = serverProperties.getServlet().getContextPath();
        String xForwardedHost = "https://my-connector";
        // Confirm connector has updated the host placeholder.
        String expectedMetadata = fromFile("/static/discovery/metadata.json")
                .replace("${CONNECTOR_HOST}", xForwardedHost)
                .replace("${CONTEXT_PATH}", appContextPath);

        // Discovery metadata.json is at the connector root.
        webClient.get()
                .uri(appContextPath + ServiceNowConstants.URL_PATH_SEPERATOR)
                .headers(ControllerTestsBase::headers)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .json(expectedMetadata)
                // Verify object type is 'botDiscovery'.
                .jsonPath("$.object_types.botDiscovery").exists();
    }

    @ParameterizedTest
    @MethodSource("inputsForGetCategoryItems")
    public void testGetCategoryItems(String servicenowResponse, String connectorResponse) throws Exception {
        final String appContextPath = serverProperties.getServlet().getContextPath();
        // Find out the id of the requested Catalog.
        findIdOfCatalog("/api/sn_sc/servicecatalog/catalogs", GET, "/botflows/servicenow/response/catalogs.json");

        // Find out the id of the category requested.
        String catalogId = "e0d08b13c3330100c8b837659bba8fb4";
        mockBackend.expect(ExpectedCount.max(2), requestToUriTemplate("/api/sn_sc/servicecatalog/catalogs/{catalogId}", catalogId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/service_catalog.json"), APPLICATION_JSON));
        String categoryId = "59f586f23731300054b6a3549dbe5db7";
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/items"
                        + "?sysparm_category={categoryId}"
                        + "&sysparm_limit=10&sysparm_offset=0",
                categoryId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile(servicenowResponse), APPLICATION_JSON));
        String body = requestObjectsGet(appContextPath + "/api/v1/device_list?device_category=Laptops&limit=10&offset=0", SNOW_AUTH_TOKEN, null)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(BotFlowTest::normalizeBotObjects)
                .map(json -> json.replaceAll(mockBackend.url("/"), "https://mock-snow.com/"))
                .block();

        assertThatJson(body).when(IGNORING_EXTRA_FIELDS, IGNORING_ARRAY_ORDER)
            .isEqualTo(fromFile(connectorResponse));
    }

    private void findIdOfCatalog(String s, HttpMethod get, String s2) throws IOException {
        mockBackend.expect(ExpectedCount.max(2), requestTo(s))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(get))
                .andRespond(withSuccess(fromFile(s2), APPLICATION_JSON));
    }

    @Test void testOrderADeviceShouldReturnDeviceCategoryList() throws Exception {
        final String appContextPath = serverProperties.getServlet().getContextPath();
        findIdOfCatalog("/api/sn_sc/servicecatalog/catalogs", GET, "/botflows/servicenow/response/catalogs.json");

        // Find out the id of the category requested.
        String catalogId = "e0d08b13c3330100c8b837659bba8fb4";
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/catalogs/{catalogId}", catalogId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/service_catalog.json"), APPLICATION_JSON));

        String body = requestObjectsGet(appContextPath + "/api/v1/deviceCategoryList", SNOW_AUTH_TOKEN, null)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(BotFlowTest::normalizeBotObjects)
                .map(json -> json.replaceAll(mockBackend.url("/"), "https://mock-snow.com/"))
                .block();

        assertThat("objects should be identical", body, sameJSONAs(fromFile("/botflows/connector/response/device_category_list.json")).allowingAnyArrayOrdering());
    }

    @Test void testViewMyTasksAction() throws Exception {
        String userEmailId = "admin@acme.com";
        final String appContextPath = serverProperties.getServlet().getContextPath();
        mockBackend.expect(requestToUriTemplate(
                "/api/now/table/task?sysparm_display_value=true&sysparm_limit=5&sysparm_offset=0"
                        + "&opened_by.email={userEmailId}&active=true&sysparm_query=ORDERBYDESCsys_created_on",
                userEmailId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/task_ticket.json"), APPLICATION_JSON));

        String body = performAction(POST, appContextPath + "/api/v1/tasks", SNOW_AUTH_TOKEN, new LinkedMultiValueMap<>())
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(res -> normalizeBotObjects(res, mockBackend.url("/")))
                .block();

        assertThat("objects should be identical", body, sameJSONAs(fromFile("/botflows/connector/response/task_ticket.json")).allowingAnyArrayOrdering());
    }

    @Test void testViewMyTasksActionShouldReturnServiceNowUrlWhenMoreThanFiveOpenTickets() throws Exception {
        String userEmailId = "admin@acme.com";
        final String appContextPath = serverProperties.getServlet().getContextPath();
        mockBackend.expect(requestToUriTemplate(
                "/api/now/table/task?sysparm_display_value=true&sysparm_limit=5&sysparm_offset=0"
                        + "&opened_by.email={userEmailId}&active=true&sysparm_query=ORDERBYDESCsys_created_on",
                userEmailId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/task_ticket_more_than_5.json"), APPLICATION_JSON));
        String body = performAction(POST, appContextPath + "/api/v1/tasks", SNOW_AUTH_TOKEN, new LinkedMultiValueMap<>())
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(res -> normalizeBotObjects(res, mockBackend.url("/")))
                .block();

        assertThat("objects should be identical", body, sameJSONAs(fromFile("/botflows/connector/response/task_ticket_more_than_5.json")).allowingAnyArrayOrdering());
    }

    @Test void testBotDiscoveryObject() throws Exception {
        final String appContextPath = serverProperties.getServlet().getContextPath();
        String body = requestObjects(appContextPath + "/bot-discovery", SNOW_AUTH_TOKEN,
                "/botflows/connector/request/bot_discovery_object.json",
                OBJ_TYPE_BOT_DISCOVERY, null)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(BotFlowTest::normalizeBotObjects)
                .block();

        assertThat("objects should be identical", body, sameJSONAs(fromFile("/botflows/connector/response/bot_discovery_object.json")).allowingAnyArrayOrdering());
        //TODO : This will taken care as part of https://jira-euc.eng.vmware.com/jira/browse/HW-110258
    }

    @Test void testCreateTaskAction() throws IOException {
        String taskType = "ticket";
        final String appContextPath = serverProperties.getServlet().getContextPath();
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

        String body = performAction(POST, appContextPath + "/api/v1/task/create", SNOW_AUTH_TOKEN, actionFormData)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(res -> normalizeBotObjects(res, mockBackend.url("/")))
                .block();

        assertThat("objects should be identical", body, sameJSONAs(fromFile("/botflows/connector/response/create_ticket.json")).allowingAnyArrayOrdering());
    }

    @Test void testAddCart() throws IOException {
        String itemId = "2ab7077237153000158bbfc8bcbe5da9"; //Macbook pro.
        Integer itemCount = 1;
        final String appContextPath = serverProperties.getServlet().getContextPath();
        mockBackend.expect(requestToUriTemplate("/api/sn_sc/servicecatalog/items/{itemId}/add_to_cart", itemId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(content().json(String.format(
                        "{"
                                + "\"sysparm_quantity\": %d"
                                + "}", itemCount)))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/add_mac_to_cart.json"), APPLICATION_JSON));

        // To deliver cart object
        expectCartRequest();


        MultiValueMap<String, String> actionFormData = new LinkedMultiValueMap<>();
        actionFormData.set("itemId", itemId);
        actionFormData.set("itemCount", String.valueOf(itemCount));

        String body = performAction(PUT, appContextPath + "/api/v1/cart", SNOW_AUTH_TOKEN, actionFormData)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(BotFlowTest::normalizeBotObjects)
                .map(json -> json.replaceAll(mockBackend.url("/"), "https://mock-snow.com/"))
                .block();

        assertThat("objects should be identical", body, sameJSONAs(fromFile("/botflows/connector/response/add_mac_to_cart.json")).allowingAnyArrayOrdering());
    }

    @Test void testClearCart() throws IOException {
        expectCartRequest();
        final String appContextPath = serverProperties.getServlet().getContextPath();
        mockBackend.expect(requestToUriTemplate(CLEAR_CART_URL, CART_ID))
                .andExpect(header(AUTHORIZATION, BEARER + SNOW_AUTH_TOKEN))
                .andExpect(method(DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        String body = performAction(DELETE, appContextPath + CART_URL_CONNECTOR, SNOW_AUTH_TOKEN, null)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(BotFlowTest::normalizeBotObjects)
                .block();
        assertThat("objects should be identical", body, sameJSONAs(fromFile(CLEAR_CART_RESP_FILE)).allowingAnyArrayOrdering());
    }

    @Test void testClearCartError() throws IOException {
        expectCartRequest();
        final String appContextPath = serverProperties.getServlet().getContextPath();
        mockBackend.expect(requestToUriTemplate(CLEAR_CART_URL, CART_ID))
                .andExpect(header(AUTHORIZATION, BEARER + SNOW_AUTH_TOKEN))
                .andExpect(method(DELETE))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(APPLICATION_JSON)
                        .body(fromFile(CLEAR_CART_ERR_FILE)));
        String body =
                performAction(DELETE, appContextPath + CART_URL_CONNECTOR, SNOW_AUTH_TOKEN, null)
                        .expectStatus().isUnauthorized()
                        .returnResult(String.class)
                        .getResponseBody()
                        .collect(Collectors.joining())
                        .map(BotFlowTest::normalizeBotObjects)
                        .block();
        assertThat("objects should be identical", body, sameJSONAs(fromFile(CLEAR_CART_CONNECTOR_RESP)).allowingAnyArrayOrdering());
    }

    @Test void testCheckout() throws IOException {
        final String appContextPath = serverProperties.getServlet().getContextPath();
        findIdOfCatalog("/api/sn_sc/servicecatalog/cart/checkout", POST, "/botflows/servicenow/response/checkout.json");

        String reqNumber = "REQ0010033"; // Checkout request ticket number.
        // To deliver task object.
        expectTaskReqByNumber("task", reqNumber, "/botflows/servicenow/response/ticket_checkout_request.json");

        String body = performAction(POST, appContextPath + "/api/v1/checkout", SNOW_AUTH_TOKEN, null)
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(res -> normalizeBotObjects(res, mockBackend.url("/")))
                .block();

        assertThat("objects should be identical", body, sameJSONAs(fromFile("/botflows/connector/response/checkout.json")).allowingAnyArrayOrdering());
    }

    @Test void testViewMyTasksActionIftasksAreEmpty() throws Exception {
        String userEmailId = "admin@acme.com";
        final String appContextPath = serverProperties.getServlet().getContextPath();
        mockBackend.expect(requestToUriTemplate(
                "/api/now/table/task?sysparm_display_value=true&sysparm_limit=5&sysparm_offset=0"
                        + "&opened_by.email={userEmailId}&active=true&sysparm_query=ORDERBYDESCsys_created_on",
                userEmailId))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/botflows/servicenow/response/task_ticket_if_empty.json"), APPLICATION_JSON));

        String body = performAction(POST, appContextPath + "/api/v1/tasks", SNOW_AUTH_TOKEN, new LinkedMultiValueMap<>())
                .expectStatus().is2xxSuccessful()
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(res -> normalizeBotObjects(res, mockBackend.url("/")))
                .block();
        int objectsCount = getObjectsCount(body);
        assertThat("object count should be 1", objectsCount, is(1));
        assertThat("objects should be identical", body, sameJSONAs(fromFile("/botflows/connector/response/task_ticket_if_empty_connector_response.json")).allowingAnyArrayOrdering());
    }

    private int getObjectsCount(String body) {
        DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(body);
        return context.read("$.objects.length()");
    }

    private void expectTaskReqByNumber(String taskType, String taskNumber, String sNowResponseFile) throws IOException {
        mockBackend.expect(requestToUriTemplate("/api/now/table/{taskType}?sysparm_display_value=true&number={taskNumber}",
                taskType, taskNumber))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile(sNowResponseFile), APPLICATION_JSON));
    }

    private void expectCartRequest() throws IOException {
        findIdOfCatalog("/api/sn_sc/servicecatalog/cart", GET, "/botflows/servicenow/response/cart.json");
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

    private WebTestClient.ResponseSpec requestObjectsGet(String objReqPath, String sNowAuthToken, String language) throws Exception {
        return doGet(
                objReqPath,
                sNowAuthToken,
                language
        );
    }

    private WebTestClient.ResponseSpec doGet(
            String path,
            String authToken,
            String language
    ) throws Exception {

        WebTestClient.RequestHeadersSpec<?> spec = webClient.get()
                .uri(path)
                .accept(APPLICATION_JSON)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(X_AUTH_HEADER, "Bearer " + authToken)
                .header("x-routing-template", "https://mf/connectors/abc123/INSERT_OBJECT_TYPE/")
                .headers(headers -> headers(headers, path));
        //.syncBody(fromFile(requestFile));

        if (StringUtils.isNotBlank(language)) {
            spec = spec.header(ACCEPT_LANGUAGE, language);
        }

        return spec.exchange();
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

    public static String normalizeBotObjects(String body) {
        Configuration configuration = Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .build();

        DocumentContext context = JsonPath.using(configuration).parse(body);
        context.set("$.objects[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID);
        // Above line can be removed, when all the bot flows move to the latest schema.
        context.set("$.objects[*].itemDetails[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID);
        context.set("$.objects[*].children[*].itemDetails[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID);
        context.set("$.objects[*].itemDetails.children[*].itemDetails[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID);
        return context.jsonString();
    }

    private static String normalizeBotObjects(String body, String backendBaseUrl) {
        return normalizeBotObjects(body.replace(backendBaseUrl, "https://dev15329.service-now.com/"));
    }
}

