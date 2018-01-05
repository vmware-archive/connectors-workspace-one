/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.jira;

import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonReplacementsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.AsyncRestTemplate;

import static com.vmware.connectors.test.JsonSchemaValidator.isValidHeroCardConnectorResponse;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Created by Rob Worsnop on 12/9/16.
 */
public class JiraControllerTests extends ControllerTestsBase {

    @Value("classpath:jira/responses/APF-27.json")
    private Resource apf27;

    @Value("classpath:jira/responses/APF-28.json")
    private Resource apf28;

    @Value("classpath:jira/responses/myself.json")
    private Resource myself;

    @Autowired
    private AsyncRestTemplate rest;

    private MockRestServiceServer mockJira;

    @Before
    public void setup() throws Exception {
        super.setup();
        mockJira = MockRestServiceServer.bindTo(rest).ignoreExpectOrder(true).build();
    }

    @Test
    public void testProtectedResource() throws Exception {
        testProtectedResource(POST, "/cards/requests");
        testProtectedResource(POST, "/api/v1/issues/1234/comment");
        testProtectedResource(POST, "/api/v1/issues/1234/watchers");
    }

    @Test
    public void testDiscovery() throws Exception {
        testConnectorDiscovery();
    }

    @Test
    public void testRequestWithEmptyIssue() throws Exception {
        testRequestCards("emptyIssue.json", "emptyIssue.json", null);
    }

    @Test
    public void testRequestCardsWithEmptyToken() throws Exception {
        testRequestCardsWithMissingParameter("emptyRequest.json", "emptyRequest.json");
    }

    @Test
    public void testRequestCardsEmpty() throws Exception {
        testRequestCardsWithMissingParameter("emptyToken.json", "emptyToken.json");
    }

    @Test
    public void testRequestCardsSuccess() throws Exception {
        expect("APF-27").andRespond(withSuccess(apf27, APPLICATION_JSON));
        expect("APF-28").andRespond(withSuccess(apf28, APPLICATION_JSON));
        testRequestCards("request.json", "success.json", null);
        mockJira.verify();
    }

    @Test
    public void testAuthSuccess() throws Exception {
        expect("XYZ-999").andRespond(withStatus(NOT_FOUND));
        perform(get("/test-auth").with(token(getAccessToken()))
                .header("x-jira-authorization", "Bearer abc")
                .header("x-jira-base-url", "https://jira.acme.com"))
                .andExpect(status().isOk());
        mockJira.verify();
    }

    @Test
    public void testAuthFail() throws Exception {
        expect("XYZ-999").andRespond(withUnauthorizedRequest());
        perform(get("/test-auth").with(token(getAccessToken()))
                .header("x-jira-authorization", "Bearer abc")
                .header("x-jira-base-url", "https://jira.acme.com"))
                .andExpect(status().isBadRequest());
        mockJira.verify();
    }

    /*
    Give more priority to x-auth header if more than one request-headers are missing.
     */
    @Test
    public void testMissingRequestHeaders() throws Exception {
        perform(post("/cards/requests").with(token(getAccessToken()))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-routing-prefix", "https://hero/connectors/jira/")
                .content(fromFile("/jira/requests/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("Missing request header 'x-jira-authorization'")));
    }

    @Test
    public void testRequestCardsSuccessI18n() throws Exception {
        expect("APF-27").andRespond(withSuccess(apf27, APPLICATION_JSON));
        expect("APF-28").andRespond(withSuccess(apf28, APPLICATION_JSON));
        testRequestCards("request.json", "success_xx.json", "xx;q=1.0");
        mockJira.verify();
    }

    @Test
    public void testRequestCardsNotAuthorized() throws Exception {
        mockJira.expect(times(1), requestTo(any(String.class)))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer bogus"))
                .andExpect(method(GET))
                .andRespond(withUnauthorizedRequest());
        perform(requestCards("bogus", "request.json"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Backend-Status", "401"))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(fromFile("/connector/responses/invalid_connector_token.json")));
        mockJira.verify();
    }

    @Test
    public void testRequestCardsOneNotFound() throws Exception {
        expect("APF-27").andRespond(withSuccess(apf27, APPLICATION_JSON));
        expect("BOGUS-999").andRespond(withStatus(NOT_FOUND));
        perform(requestCards("abc", "oneCardNotFound.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().string(isValidHeroCardConnectorResponse()))
                .andExpect(content().string(JsonReplacementsBuilder.from(
                        fromFile("connector/responses/APF-27.json")).buildForCards()));
        mockJira.verify();
    }

    @Test
    public void testRequestCardsOneServerError() throws Exception {
        expect("POISON-PILL").andRespond(withServerError());
        perform(requestCards("abc", "oneServerError.json"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("X-Backend-Status", "500"));
        mockJira.verify();
    }

    @Test
    public void testAddComment() throws Exception {
        mockJira.expect(requestTo("https://jira.acme.com/rest/api/2/issue/1234/comment"))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().string("{\"body\":\"Hello\"}"))
                .andRespond(withStatus(CREATED));

        perform(post("/api/v1/issues/1234/comment").with(token(getAccessToken()))
                .contentType(APPLICATION_FORM_URLENCODED)
                .header("x-jira-authorization", "Bearer abc")
                .header("x-jira-base-url", "https://jira.acme.com")
                .content("body=Hello"))
                .andExpect(status().isCreated());
        mockJira.verify();
    }

    @Test
    public void testAddCommentWith401() throws Exception {
        perform(post("/api/v1/issues/1234/comment")
                .contentType(APPLICATION_FORM_URLENCODED)
                .header("x-jira-authorization", "Bearer abc")
                .header("x-jira-base-url", "https://jira.acme.com")
                .content("body=Hello"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"unauthorized\"}"));
        mockJira.verify();
    }

    @Test
    public void testAddCommentWithMissingConnectorAuthorization() throws Exception {
        perform(post("/api/v1/issues/1234/comment").with(token(getAccessToken()))
                .contentType(APPLICATION_FORM_URLENCODED)
                .header("x-jira-base-url", "https://jira.acme.com")
                .content("body=Hello"))
                .andExpect(status().isBadRequest());
        mockJira.verify();
    }

    @Test
    public void testAddCommentWithBackend401() throws Exception {
        mockJira.expect(requestTo("https://jira.acme.com/rest/api/2/issue/1234/comment"))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer bogus"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().string("{\"body\":\"Hello\"}"))
                .andRespond(withStatus(UNAUTHORIZED));

        perform(post("/api/v1/issues/1234/comment").with(token(getAccessToken()))
                .contentType(APPLICATION_FORM_URLENCODED)
                .header("x-jira-authorization", "Bearer bogus")
                .header("x-jira-base-url", "https://jira.acme.com")
                .content("body=Hello"))
                .andExpect(status().isBadRequest())
                .andExpect((content().json(fromFile("/connector/responses/invalid_connector_token.json"))));
        mockJira.verify();
    }

    @Test
    public void testAddWatcher() throws Exception {
        mockJira.expect(requestTo("https://jira.acme.com/rest/api/2/myself"))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"))
                .andExpect(method(GET))
                .andRespond(withSuccess(myself, APPLICATION_JSON));
        mockJira.expect(requestTo("https://jira.acme.com/rest/api/2/issue/1234/watchers"))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().string("\"harshas\""))
                .andRespond(withStatus(NO_CONTENT));

        perform(post("/api/v1/issues/1234/watchers").with(token(getAccessToken()))
                .header("x-jira-authorization", "Bearer abc")
                .header("x-jira-base-url", "https://jira.acme.com"))
                .andExpect(status().isNoContent());
        mockJira.verify();
    }

    @Test
    public void testAddWatcherWith401() throws Exception {
        perform(post("/api/v1/issues/1234/watchers")
                .header("x-jira-authorization", "Bearer abc")
                .header("x-jira-base-url", "https://jira.acme.com"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"unauthorized\"}"));
        mockJira.verify();
    }

    @Test
    public void testAddWatcherWithMissingConnectorAuthorization() throws Exception {
        perform(post("/api/v1/issues/1234/watchers").with(token(getAccessToken()))
                .header("x-jira-base-url", "https://jira.acme.com"))
                .andExpect(status().isBadRequest());
        mockJira.verify();
    }

    @Test
    public void testAddWatcherWithBackend401() throws Exception {
        mockJira.expect(requestTo("https://jira.acme.com/rest/api/2/myself"))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer bogus"))
                .andExpect(method(GET))
                .andRespond(withStatus(UNAUTHORIZED));

        perform(post("/api/v1/issues/1234/watchers").with(token(getAccessToken()))
                .header("x-jira-authorization", "Bearer bogus")
                .header("x-jira-base-url", "https://jira.acme.com"))
                .andExpect(status().isBadRequest())
                .andExpect((content().json(fromFile("/connector/responses/invalid_connector_token.json"))));
        mockJira.verify();
    }

    @Test
    public void testGetImage() throws Exception {
        perform(get("/images/connector.png"))
                .andExpect(status().isOk())
                .andExpect(header().longValue(CONTENT_LENGTH, 11851))
                .andExpect(header().string(CONTENT_TYPE, IMAGE_PNG_VALUE))
                .andExpect((content().bytes(bytesFromFile("/static/images/connector.png"))));
    }

    private ResponseActions expect(String issue) {
        return mockJira.expect(requestTo("https://jira.acme.com/rest/api/2/issue/" + issue))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"));
    }

    private void testRequestCards(String requestFile, String responseFile, String acceptLanguage) throws Exception {
        MockHttpServletRequestBuilder builder = requestCards("abc", requestFile);
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

    private void testRequestCardsWithMissingParameter(String requestFile, String responseFile) throws Exception {
        MockHttpServletRequestBuilder builder = requestCards("abc", requestFile);

        perform(builder)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(fromFile("connector/responses/" + responseFile)));
    }

    private MockHttpServletRequestBuilder requestCards(String authToken, String requestfile) throws Exception {
        return post("/cards/requests").with(token(getAccessToken()))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-jira-authorization", "Bearer " + authToken)
                .header("x-jira-base-url", "https://jira.acme.com")
                .header("x-routing-prefix", "https://hero/connectors/jira/")
                .content(fromFile("/jira/requests/" + requestfile));
    }
}
