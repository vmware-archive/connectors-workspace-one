/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch;

import com.google.common.collect.ImmutableList;
import com.vmware.connectors.mock.MockWebServerWrapper;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_UTF8;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

/**
 * Created by harshas on 9/20/17.
 */
@TestPropertySource(locations = {"classpath:app.properties"})
@ContextConfiguration(initializers = AirWatchControllerTest.CustomInitializer.class)
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

    private static MockWebServerWrapper mockGreenbox;

    @BeforeAll
    static void createMock() {
        mockGreenbox = new MockWebServerWrapper(new MockWebServer());
    }

    @BeforeEach
    void resetGreenbox() {
        mockGreenbox.reset();
    }

    @AfterEach
    void verifyMock() {
        mockGreenbox.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/mdm/app/install"})
    void testProtectedResource(String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @Test
    void testDiscovery() throws IOException {
        webClient.get()
                .uri("/")
                .headers(ControllerTestsBase::headers)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody().json(fromFile("/connector/responses/discovery.json"));

        String expectedMetadata = fromFile("/connector/responses/metadata.json");
        String localHostRegex = "(http|https):\\/\\/localhost:\\d{4,5}";
        webClient.get()
                .uri("/discovery/metadata.json")
                .headers(ControllerTestsBase::headers)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .consumeWith(res -> {
                    String body = new String(res.getResponseBody());
                    assertThat(body.replaceAll(localHostRegex, "http://localhost"),
                            sameJSONAs(expectedMetadata).allowingAnyArrayOrdering());
                })
                // Confirm connector has updated the place holder.
                .jsonPath("$.object_types[?(@.endpoint.href =~ /.*" + localHostRegex + ".*$/i)]").isNotEmpty()
                // Verify object type is 'card'.
                .jsonPath("$.object_types[0].object_type.name").isEqualTo("card");
    }

    @Test
    void testRegex1() throws Exception {
        List<String> expected = ImmutableList.of(
                "BoXeR",
                "POISON",
                "concur"
        );
        testRegex("app_keywords", fromFile("/regex/email1.txt"), expected);
    }

    @Test
    void testRegex2() throws Exception {
        List<String> expected = ImmutableList.of(
                "boxer",
                "poison",
                "concur"
        );
        testRegex("app_keywords", fromFile("/regex/email2.txt"), expected);
    }

    @Test
    void testRegex3() throws Exception {
        List<String> expected = ImmutableList.of(
                "poison",
                "concur"
        );
        testRegex("app_keywords", fromFile("/regex/email3.txt"), expected);
    }

    @DisplayName("Card request success cases")
    @ParameterizedTest(name = "{index} ==> Request=''{0}'', Language=''{1}''")
    @CsvSource({
            "request.json, " + StringUtils.EMPTY + ", success.json",
            "request.json, xx;q=1.0, success_xx.json",
            "requestDuplicate.json, " + StringUtils.EMPTY + ", success.json"})
        // Expect DS request only once for each app.
    void testRequestCardsSuccess(String requestFile, String acceptLanguage, String responseFile) throws Exception {
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.android.boxer")
                .andRespond(withSuccess(awAppNotInstalled, APPLICATION_JSON));
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.concur.breeze")
                .andRespond(withSuccess(awAppInstalled, APPLICATION_JSON));
        testRequestCards(requestFile, responseFile, acceptLanguage);
    }

    /*
     * Boxer - Not installed.
     * Concur - Installed
     * customer support - Zendesk is not configured for iOS.
     */
    @Test
    void testRequestCardsSuccessiOS() throws Exception {
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.air-watch.boxer")
                .andRespond(withSuccess(awAppNotInstalled, APPLICATION_JSON));
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.concur.concurmobile")
                .andRespond(withSuccess(awAppInstalled, APPLICATION_JSON));
        testRequestCards("requestiOS.json", "successiOS.json", null);
    }

    @Test
    void testInstallAction() throws IOException{

        expectGBSessionRequests("android");

        // Search for app "Concur"
        String searchApp = IOUtils.toString(gbSearchApp.getInputStream(), Charset.defaultCharset())
                .replaceAll("\\$\\{greenbox.url}", mockGreenbox.url(""));
        mockGreenbox.expect(requestTo("/catalog-portal/services/api/entitlements?q=Concur"))
                .andExpect(method(GET))
                .andExpect(header(COOKIE, gbCatalogContextCookies("euc123", null)))
                .andRespond(withSuccess().body(searchApp).contentType(HAL_JSON_UTF8));

        // Trigger install for "MDM-134-Native-Public".
        mockGreenbox.expect(requestTo("/catalog-portal/services/api/activate/MDM-134-Native-Public"))
                .andExpect(method(POST))
                .andExpect(header(COOKIE, gbCatalogContextCookies("euc123", "csrf123")))
                .andExpect(header("X-XSRF-TOKEN", "csrf123"))
                .andRespond(withSuccess().body(gbInstallApp).contentType(HAL_JSON_UTF8));

        webClient.post()
                .uri("/mdm/app/install")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header("x-airwatch-base-url", mockBackend.url(""))
                .body(BodyInserters.fromFormData("app_name", "Concur")
                        .with("udid", "ABCD")
                        .with("platform", "android"))
                .exchange()
                .expectStatus().isOk();
    }

    @ParameterizedTest(name = "{index} ==> GB Response=''{1}''")
    @CsvSource({
            "Browser, searchAppMultipleFound.json",
            "unassignedMdmApp, searchAppNotFound.json"})
    void testInstallActionBadRequest(String appName, String gbResponseFile) throws Exception {

        expectGBSessionRequests("Apple");

        mockGreenbox.expect(requestTo("/catalog-portal/services/api/entitlements?q=" + appName))
                .andExpect(method(GET))
                .andExpect(header(COOKIE, gbCatalogContextCookies("euc123", null)))
                .andRespond(withSuccess().body(fromFile("greenbox/responses/" + gbResponseFile))
                        .contentType(HAL_JSON_UTF8));

        webClient.post()
                .uri("/mdm/app/install")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header("x-airwatch-base-url", mockBackend.url(""))
                .body(BodyInserters.fromFormData("app_name", appName)
                        .with("udid", "ABCD")
                        .with("platform", "ios"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testMissingRequestHeaders() throws Exception {
        webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-routing-prefix", "https://hero/connectors/airwatch/")
                .syncBody(fromFile("/connector/requests/request.json"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message")
                    .isEqualTo("Missing request header 'x-airwatch-base-url' for method parameter of type String");

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
        mockBackend.expect(times(2), requestTo(any(String.class)))
                .andExpect(header(AUTHORIZATION, "Bearer " + accessToken()))
                .andExpect(method(GET))
                .andRespond(withStatus(FORBIDDEN).body(awUserForbidden));
        requestCards("request.json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(fromFile("connector/responses/forbiddenUdid.json")); }

    @Test
    void testRequestCardsOneServerError() throws Exception {
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.poison.pill")
                .andRespond(withServerError());
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.android.boxer")
                .andRespond(withSuccess(awAppNotInstalled, APPLICATION_JSON));
        requestCards("oneServerError.json")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals("X-Backend-Status", "500");
    }

    @Test
    void testRequestCardsInvalidUdid() throws Exception {
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=INVALID&BundleId=com.android.boxer")
                .andRespond(withStatus(NOT_FOUND).body(fromFile("airwatch/responses/udidNotFound.json")));
        requestCards("invalidUdid.json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(fromFile("connector/responses/invalidUdid.json"));
    }

    @Test
    void testRequestForInvalidPlatform() throws Exception {
        requestCards("invalidPlatform.json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(fromFile("connector/responses/invalidPlatform.json"));

    }

    private void testRequestCards(String requestFile, String responseFile, String acceptLanguage) throws Exception {
        WebTestClient.RequestHeadersSpec<?> spec = requestCards(requestFile);
        if (acceptLanguage != null) {
            spec = spec.header(ACCEPT_LANGUAGE, acceptLanguage);
        }
        String body = spec.exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block();
        assertThat(body,  sameJSONAs(fromFile("connector/responses/" + responseFile)).allowingAnyArrayOrdering());
     }

    private WebTestClient.RequestHeadersSpec<?> requestCards(String requestfile) throws IOException {
        return webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-airwatch-base-url", mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/airwatch/")
                .syncBody(fromFile("/connector/requests/" + requestfile));
    }

    private ResponseActions expectAWRequest(String uri) {
        return mockBackend.expect(requestTo(uri))
                .andExpect(method(GET))
                .andExpect(header(AUTHORIZATION, "Bearer " + accessToken()));
    }

    private void expectGBSessionRequests(String deviceType) {
        // eucToken
        mockGreenbox.expect(requestTo("/catalog-portal/services/auth/eucTokens?deviceUdid=ABCD&deviceType=" + deviceType))
                .andExpect(method(POST))
                .andExpect(header(COOKIE, "HZN=" + accessToken()))
                .andRespond(withStatus(CREATED).body(gbEucToken).contentType(HAL_JSON_UTF8));

        // CSRF token
        HttpHeaders csrfHeaders = new HttpHeaders();
        csrfHeaders.set(SET_COOKIE, "EUC_XSRF_TOKEN=csrf123;Path=/catalog-portal;Secure");
        mockGreenbox.expect(requestTo("/catalog-portal/"))
                .andExpect(method(OPTIONS))
                .andExpect(header(COOKIE, gbCatalogContextCookies("euc123", null)))
                .andRespond(withSuccess().headers(csrfHeaders));
    }

    private String[] gbCatalogContextCookies(String euc, String csrf) {
        String eucCookie = "USER_CATALOG_CONTEXT=" + euc;
        String csrfCookie = Optional.ofNullable(csrf).map(cookie -> "EUC_XSRF_TOKEN=" + cookie).orElse(null);
        return Stream.of(eucCookie, csrfCookie)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }

    public static class CustomInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            String gbUrl = AirWatchControllerTest.mockGreenbox.url("");
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext, "greenbox.url=" + gbUrl);
        }
    }
}
