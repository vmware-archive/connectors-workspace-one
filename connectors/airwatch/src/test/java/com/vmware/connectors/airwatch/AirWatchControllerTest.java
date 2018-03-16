/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch;

import com.vmware.connectors.mock.MockRestServiceServer;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonReplacementsBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.vmware.connectors.test.JsonSchemaValidator.isValidHeroCardConnectorResponse;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Created by harshas on 9/20/17.
 */
@TestPropertySource(locations = {"classpath:app.properties"})
class AirWatchControllerTest extends ControllerTestsBase {

    @Value("classpath:airwatch/responses/awAppInstalled.json")
    private Resource awAppInstalled;

    @Value("classpath:airwatch/responses/awAppNotInstalled.json")
    private Resource awAppNotInstalled;

    @Value("classpath:airwatch/responses/awUserForbidden.json")
    private Resource awUserForbidden;

    @Value("classpath:greenbox/responses/eucToken.json")
    private Resource gbEucToken;

    @Value("classpath:greenbox/responses/searchApp.json")
    private Resource gbSearchApp;

    @Value("classpath:greenbox/responses/installApp.json")
    private Resource gbInstallApp;

    private com.vmware.connectors.mock.MockRestServiceServer mockBackend;

    private final static String AIRWATCH_BASE_URL = "https://air-watch.acme.com";
    private final static String GREENBOX_BASE_URL = "https://herocard.vmwareidentity.com";

    @BeforeEach
    void init() throws Exception {
        super.setup();
        mockBackend = MockRestServiceServer.bindTo(requestHandlerHolder).ignoreExpectOrder(true).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/mdm/app/install"})
    void testProtectedResource(String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @Test
    void testDiscovery() throws Exception {
        perform(request(GET, "/"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fromFile("/connector/responses/discovery.json")));
        perform(request(GET, "/discovery/metadata.json"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fromFile("/connector/responses/metadata.json")));
    }

    @DisplayName("Card request success cases")
    @ParameterizedTest(name = "{index} ==> Language=''{0}''")
    @CsvSource({
            StringUtils.EMPTY + ", success.json",
            "xx;q=1.0, success_xx.json"})
    void testRequestCardsSuccess(String acceptLanguage, String responseFile) throws Exception {
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.android.boxer")
                .andRespond(withSuccess(awAppNotInstalled, APPLICATION_JSON));
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.concur.breeze")
                .andRespond(withSuccess(awAppInstalled, APPLICATION_JSON));
        testRequestCards("request.json", responseFile, acceptLanguage);
    }

    @Test
    void testInstallAction() throws Exception {

        expectGBSessionRequests();

        // Search for app "Concur"
        expectGBRequest(
                "/catalog-portal/services/api/entitlements?q=Concur",
                GET, gbCatalogContextCookie("euc123", null))
                .andRespond(withSuccess().body(gbSearchApp).contentType(APPLICATION_JSON));

        // Trigger install for "MDM-134-Native-Public".
        expectGBRequest(
                "/catalog-portal/services/api/activate/MDM-134-Native-Public",
                POST, gbCatalogContextCookie("euc123", "csrf123"))
                .andExpect(MockRestRequestMatchers.header("X-XSRF-TOKEN", "csrf123"))
                .andRespond(withSuccess().body(gbInstallApp).contentType(APPLICATION_JSON));

        perform(post("/mdm/app/install").with(token(accessToken()))
                .contentType(APPLICATION_FORM_URLENCODED)
                .header("x-airwatch-base-url", AIRWATCH_BASE_URL)
                .param("app_name", "Concur")
                .param("udid", "ABCD")
                .param("platform", "android"))
                .andExpect(status().isOk());

        mockBackend.verify();
    }

    @Test
    void testMissingRequestHeaders() throws Exception {
        perform(post("/cards/requests").with(token(accessToken()))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-routing-prefix", "https://hero/connectors/airwatch/")
                .content(fromFile("/connector/requests/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("Missing request header 'x-airwatch-base-url'")));
    }

    @ParameterizedTest
    @CsvSource({
            "emptyAppName.json, emptyCard.json",
            "bogusAppName.json, emptyCard.json"})
    void testRequestInvalidAppName(String requestFile, String responseFile) throws Exception {
        testRequestCards(requestFile, responseFile, null);
    }

    /*
     * User might try to check someone else's app status.
     */
    @Test
    void testRequestCardsForbidden() throws Exception {
        mockBackend.expect(times(1), requestTo(any(String.class)))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer " + accessToken()))
                .andExpect(method(GET))
                .andRespond(withStatus(FORBIDDEN).body(awUserForbidden));
        perform(requestCards("request.json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(fromFile("connector/responses/forbiddenUdid.json")));
        mockBackend.verify();
    }

    @Test
    void testRequestCardsOneServerError() throws Exception {
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.poison.pill")
                .andRespond(withServerError());
        perform(requestCards("oneServerError.json"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("X-Backend-Status", "500"));
        mockBackend.verify();
    }

    @Test
    void testRequestCardsInvalidUdid() throws Exception {
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=INVALID&BundleId=com.android.boxer")
                .andRespond(withStatus(NOT_FOUND).body(fromFile("airwatch/responses/udidNotFound.json")));
        perform(requestCards("invalidUdid.json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(fromFile("connector/responses/invalidUdid.json")));
    }

    @Test
    void testRequestForInvalidPlatform() throws Exception {
        perform(requestCards("invalidPlatform.json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(fromFile("connector/responses/invalidPlatform.json")));

    }

    private void testRequestCards(String requestFile, String responseFile, String acceptLanguage) throws Exception {
        MockHttpServletRequestBuilder builder = requestCards(requestFile);
        if (acceptLanguage != null) {
            builder = builder.header(ACCEPT_LANGUAGE, acceptLanguage);
        }
        perform(builder)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().string(isValidHeroCardConnectorResponse()))
                .andExpect(content().string(JsonReplacementsBuilder.from(
                        fromFile("connector/responses/" + responseFile)).buildForCards()));
    }

    private MockHttpServletRequestBuilder requestCards(String requestfile) throws Exception {
        return post("/cards/requests").with(token(accessToken()))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-airwatch-base-url", AIRWATCH_BASE_URL)
                .header("x-routing-prefix", "https://hero/connectors/airwatch/")
                .header(HttpHeaders.HOST, "airwatch-connector")
                .content(fromFile("/connector/requests/" + requestfile));
    }

    private ResponseActions expectAWRequest(String uri) {
        return mockBackend.expect(requestTo(AIRWATCH_BASE_URL + uri))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer " + accessToken()));
    }

    private void expectGBSessionRequests() {
        // eucToken
        expectGBRequest(
                "/catalog-portal/services/auth/eucTokens?deviceUdid=ABCD&deviceType=android",
                POST, gbHZNCookie(accessToken()))
                .andRespond(withStatus(CREATED).body(gbEucToken).contentType(APPLICATION_JSON));

        // CSRF token
        HttpHeaders csrfHeaders = new HttpHeaders();
        csrfHeaders.set(SET_COOKIE, "EUC_XSRF_TOKEN=csrf123;Path=/catalog-portal;Secure");
        expectGBRequest(
                "/catalog-portal/", OPTIONS,
                gbCatalogContextCookie("euc123", null))
                .andRespond(withSuccess().headers(csrfHeaders));
    }

    private ResponseActions expectGBRequest(String uri, HttpMethod reqMethod, String cookie) {
        return mockBackend.expect(requestTo(GREENBOX_BASE_URL + uri))
                .andExpect(method(reqMethod))
                .andExpect(MockRestRequestMatchers.header(COOKIE, cookie));
    }

    private String gbHZNCookie(String hzn) {
        return "HZN=" + hzn;
    }

    private String gbCatalogContextCookie(String euc, String csrf) {
        String contextCookie = "USER_CATALOG_CONTEXT=" + euc;
        if (csrf != null) {
            return contextCookie + "; EUC_XSRF_TOKEN=" + csrf;
        }
        return contextCookie;
    }
}
