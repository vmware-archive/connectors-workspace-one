/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch;

import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonReplacementsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.AsyncRestTemplate;

import static com.vmware.connectors.test.JsonSchemaValidator.isValidHeroCardConnectorResponse;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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
public class AirWatchControllerTests extends ControllerTestsBase {

    @Value("classpath:airwatch/responses/awAppInstalled.json")
    private Resource awAppInstalled;

    @Value("classpath:airwatch/responses/awAppNotInstalled.json")
    private Resource awAppNotInstalled;

    @Value("classpath:airwatch/responses/awUserForbidden.json")
    private Resource awUserForbidden;

    @Autowired
    private AsyncRestTemplate rest;

    private MockRestServiceServer mockAirWatch;

    @Before
    public void setup() throws Exception {
        super.setup();
        mockAirWatch = MockRestServiceServer.bindTo(rest).ignoreExpectOrder(true).build();
    }

    @Test
    public void testProtectedResource() throws Exception {
        testProtectedResource(POST, "/cards/requests");
    }

    @Test
    public void testDiscovery() throws Exception {
        perform(request(GET, "/"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fromFile("/connector/responses/discovery.json")));
        perform(request(GET, "/discovery/metadata.hal"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fromFile("/connector/responses/metadata.hal")));
    }

    @Test
    public void testRequestCardsSuccess() throws Exception {
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.android.boxer")
                .andRespond(withSuccess(awAppNotInstalled, APPLICATION_JSON));
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.concur.breeze")
                .andRespond(withSuccess(awAppInstalled, APPLICATION_JSON));
        testRequestCards("request.json", "success.json", null);
        mockAirWatch.verify();
    }

    @Test
    public void testRequestCardsSuccessI18n() throws Exception {
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.android.boxer")
                .andRespond(withSuccess(awAppNotInstalled, APPLICATION_JSON));
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.concur.breeze")
                .andRespond(withSuccess(awAppInstalled, APPLICATION_JSON));
        testRequestCards("request.json", "success_xx.json", "xx;q=1.0");
        mockAirWatch.verify();
    }

    @Test
    public void testInstallAction() throws Exception {
        // ToDo:
    }

    @Test
    public void testMissingRequestHeaders() throws Exception {
        perform(post("/cards/requests").with(token(accessToken()))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-routing-prefix", "https://hero/connectors/airwatch/")
                .content(fromFile("/connector/requests/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("Missing request header 'x-airwatch-base-url'")));
    }

    @Test
    public void testRequestEmptyAppName() throws Exception {
        testRequestCards("emptyAppName.json", "emptyCard.json", null);
    }

    /*
    No card is returned for client requesting for unmanaged apps.
     */
    @Test
    public void testRequestBogusAppName() throws Exception {
        testRequestCards("bogusAppName.json", "emptyCard.json", null);
    }

    /*
    User might try to check someone else's app status.
     */
    @Test
    public void testRequestCardsForbidden() throws Exception {
        mockAirWatch.expect(times(1), requestTo(any(String.class)))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer " + accessToken()))
                .andExpect(method(GET))
                .andRespond(withStatus(FORBIDDEN).body(awUserForbidden));
        perform(requestCards("request.json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(fromFile("connector/responses/forbiddenUdid.json")));
        mockAirWatch.verify();
    }

    @Test
    public void testRequestCardsOneServerError() throws Exception {
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.android.boxer")
                .andRespond(withSuccess(awAppNotInstalled, APPLICATION_JSON));
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=ABCD&BundleId=com.poison.pill")
                .andRespond(withServerError());
        perform(requestCards("oneServerError.json"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("X-Backend-Status", "500"));
        mockAirWatch.verify();
    }

    @Test
    public void testRequestCardsInvalidUdid() throws Exception {
        expectAWRequest("/deviceservices/AppInstallationStatus?Udid=INVALID&BundleId=com.android.boxer")
                .andRespond(withStatus(NOT_FOUND).body(fromFile("airwatch/responses/udidNotFound.json")));
        perform(requestCards("invalidUdid.json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(fromFile("connector/responses/invalidUdid.json")));
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
                .header("x-airwatch-base-url", "https://air-watch.acme.com")
                .header("x-routing-prefix", "https://hero/connectors/airwatch")
                .content(fromFile("/connector/requests/" + requestfile));
    }

    private ResponseActions expectAWRequest(String uri) {
        return mockAirWatch.expect(requestTo("https://air-watch.acme.com" + uri))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer " + accessToken()));
    }
}
