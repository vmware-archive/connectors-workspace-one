/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonReplacementsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.connectors.test.JsonSchemaValidator.isValidHeroCardConnectorResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ConcurControllerTests extends ControllerTestsBase {

    private static final String REPORT_ID_1 = "79D89435DAE94F53BF60";
    private static final String REPORT_ID_2 = "F49BD54084CE4C09BD65";

    @Autowired
    private AsyncRestTemplate rest;

    private MockRestServiceServer mockConcur;

    @Value("classpath:concur/responses/report_id_1.json")
    private Resource reportId1;

    @Value("classpath:concur/responses/report_id_2.json")
    private Resource reportId2;

    @Value("classpath:concur/responses/approved.xml")
    private Resource approved;

    @Value("classpath:concur/responses/rejected.xml")
    private Resource rejected;

    @Before
    public void setup() throws Exception {
        super.setup();
        this.mockConcur = MockRestServiceServer.bindTo(rest).ignoreExpectOrder(true).build();
    }

    @Test
    public void testProtectedResources() throws Exception {
        testProtectedResource(POST, "/cards/requests");
        testProtectedResource(POST, "/api/expense/approve/" + REPORT_ID_1);
        testProtectedResource(POST, "/api/expense/reject/" + REPORT_ID_2);
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
    public void testGetImage() throws Exception {
        perform(get("/images/connector.png"))
                .andExpect(status().isOk())
                .andExpect(header().longValue(CONTENT_LENGTH, 7601))
                .andExpect(header().string(CONTENT_TYPE, IMAGE_PNG_VALUE))
                .andExpect((content().bytes(bytesFromFile("/static/images/connector.png"))));
    }

    @Test
    public void testRequestCardsSuccess() throws Exception {
        expect(REPORT_ID_1).andRespond(withSuccess(reportId1, APPLICATION_JSON));
        expect(REPORT_ID_2).andRespond(withSuccess(reportId2, APPLICATION_JSON));

        testRequestCards("request.json", "success.json", null);

        this.mockConcur.verify();
    }

    @Test
    public void testApproveRequest() throws Exception {
        expect(REPORT_ID_1).andRespond(withSuccess(reportId1, APPLICATION_JSON));

        mockExpenseReport("/api/expense/approve/",
                REPORT_ID_1,
                approved,
                "https://implementation.concursolutions.com/api/expense/expensereport/v1.1/report/gWujNPAb67r9LjhqgN7BEYYaQOWzavXBtUP1sej$sXfPQ/WorkFlowAction");
    }

    @Test
    public void testRejectRequest() throws Exception {
        expect(REPORT_ID_2).andRespond(withSuccess(reportId2, APPLICATION_JSON));

        mockExpenseReport("/api/expense/reject/",
                REPORT_ID_2,
                rejected,
                "https://implementation.concursolutions.com/api/expense/expensereport/v1.1/report/gWujNPAb67r9IgBSjNrBNbeHbgDcmoJIs2kyBQX8YzEoS/WorkFlowAction");
    }

    private void mockExpenseReport(final String uri,
                                   final String expenseReportId,
                                   final Resource expectedResponse,
                                   final String workflowActionUrl) throws Exception {
        this.mockConcur.expect(requestTo(workflowActionUrl))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "OAuth 0_xxxxEKPk8cnYlWaos22OpPsLk="))
                .andExpect(MockRestRequestMatchers.header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_XML))
                .andRespond(withSuccess(expectedResponse, APPLICATION_XML));

        perform(post(uri + expenseReportId)
                .with(token(accessToken()))
                .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                .header("x-concur-authorization", "OAuth " + "0_xxxxEKPk8cnYlWaos22OpPsLk=")
                .header("x-concur-base-url", "https://implementation.concursolutions.com")
                .param(ConcurConstants.RequestParam.REASON, "Approval Done"))
                .andExpect(status().isOk());

        this.mockConcur.verify();
    }

    @Test
    public void testEscapeHtmlTest() throws Exception {
        expect(REPORT_ID_2).andRespond(withSuccess(reportId2, APPLICATION_JSON));

        this.mockConcur.expect(requestTo("https://implementation.concursolutions.com/api/expense/expensereport/v1.1/report/gWujNPAb67r9IgBSjNrBNbeHbgDcmoJIs2kyBQX8YzEoS/WorkFlowAction"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "OAuth 0_xxxxEKPk8cnYlWaos22OpPsLk="))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_XML))
                // Html characters are escaped.
                .andExpect(MockRestRequestMatchers.content().xml("<WorkflowAction xmlns=\"http://www.concursolutions.com/api/expense/expensereport/2011/03\">\n" +
                        "    <Action>Send Back to Employee</Action>\n" +
                        "    <Comment>Approval &lt;/comment&gt; &lt;html&gt; &lt;/html&gt; Done</Comment>\n" +
                        "</WorkflowAction>\n"))
                .andRespond(withSuccess(approved, APPLICATION_XML));

        perform(post("/api/expense/reject/" + REPORT_ID_2)
                .with(token(accessToken()))
                .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                .header("x-concur-authorization", "OAuth " + "0_xxxxEKPk8cnYlWaos22OpPsLk=")
                .header("x-concur-base-url", "https://implementation.concursolutions.com")
                // Reason with html character embedded in it.
                .param(ConcurConstants.RequestParam.REASON, "Approval </comment> <html> </html> Done"))
                .andExpect(status().isOk());

        this.mockConcur.verify();
    }

    @Test
    public void testApproveExpenseReportWithUnauthorized() throws Exception {
        testUnauthorizedRequest("/api/expense/approve/");
    }

    @Test
    public void testRejectExpenseReportWithUnauthorized() throws Exception {
        testUnauthorizedRequest("/api/expense/reject/");
    }

    @Test
    public void testConcurRegex() throws Exception {
        final String concurRegex = "Report\\s*Id\\s*:\\s*([A-Za-z0-9]{20,})";
        final Pattern pattern = Pattern.compile(concurRegex);

        final List<String> expectedList = Arrays.asList(
                "523EEB33D8E548C1B90C",
                "623EEB33D8E548C1B902",
                "126EEB33D8E548C1B902",
                "356EEB33D8E548C1B902",
                "923EFB33D8E548C1B902");

        final String regexInput = fromFile("concur/regex-input.txt");
        final List<String> result = new ArrayList<>();
        for (final String input: regexInput.split("\\n")) {
            final Matcher matcher = pattern.matcher(input);
            while (matcher.find()) {
                result.add(matcher.group(1));
            }
        }
        assertThat(expectedList, equalTo(result));
    }

     private void testUnauthorizedRequest(final String uri) throws Exception {
        expect(REPORT_ID_1).andRespond(withSuccess(reportId1, APPLICATION_JSON));

        this.mockConcur.expect(requestTo("https://implementation.concursolutions.com/api/expense/expensereport/v1.1/report/gWujNPAb67r9LjhqgN7BEYYaQOWzavXBtUP1sej$sXfPQ/WorkFlowAction"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "OAuth 0_xxxxEKPk8cnYlWaos22OpPsLk="))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        perform(post(uri + REPORT_ID_1)
                .with(token(accessToken()))
                .contentType(APPLICATION_FORM_URLENCODED_VALUE)
                .header("x-concur-authorization", "OAuth " + "0_xxxxEKPk8cnYlWaos22OpPsLk=")
                .header("x-concur-base-url", "https://implementation.concursolutions.com")
                .param(ConcurConstants.RequestParam.REASON, "Approval Done"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(fromFile("/connector/responses/invalid_connector_token.json")));

        this.mockConcur.verify();
    }

    private void testRequestCardsWithMissingParameter(final String requestFile, final String responseFile) throws Exception {
        MockHttpServletRequestBuilder builder = requestCards("0_xxxxEKPk8cnYlWaos22OpPsLk=", requestFile);

        perform(builder)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(fromFile("connector/responses/" + responseFile)));
    }

    private void testRequestCards(final String requestFile,
                                  final String responseFile,
                                  final String acceptLanguage) throws Exception {
        MockHttpServletRequestBuilder builder = requestCards("0_xxxxEKPk8cnYlWaos22OpPsLk=", requestFile);
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

    private ResponseActions expect(final String issue) {
        return this.mockConcur.expect(requestTo("https://implementation.concursolutions.com/api/expense/expensereport/v2.0/report/" + issue))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "OAuth 0_xxxxEKPk8cnYlWaos22OpPsLk="))
                .andExpect(MockRestRequestMatchers.header(ACCEPT, APPLICATION_JSON_VALUE));
    }

    private MockHttpServletRequestBuilder requestCards(final String authToken, final String requestFile) throws Exception {
        return post("/cards/requests").with(token(accessToken()))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-concur-authorization", "OAuth " + authToken)
                .header("x-concur-base-url", "https://implementation.concursolutions.com")
                .header("x-routing-prefix", "https://hero/connectors/concur/")
                .content(fromFile("/concur/requests/" + requestFile));
    }
}
