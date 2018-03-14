/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

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
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ConcurControllerTest extends ControllerTestsBase {

    private static final String REPORT_ID_1 = "79D89435DAE94F53BF60";
    private static final String REPORT_ID_2 = "F49BD54084CE4C09BD65";

    private MockRestServiceServer mockConcur;

    @Value("classpath:concur/responses/report_id_1.json")
    private Resource reportId1;

    @Value("classpath:concur/responses/report_id_2.json")
    private Resource reportId2;

    @Value("classpath:concur/responses/approved.xml")
    private Resource approved;

    @Value("classpath:concur/responses/rejected.xml")
    private Resource rejected;

    @BeforeEach
    public void setup() throws Exception {
        super.setup();
        this.mockConcur = MockRestServiceServer.bindTo(requestHandlerHolder).ignoreExpectOrder(true).build();
    }

    @Test
    void testProtectedResources() throws Exception {
        testProtectedResource(POST, "/cards/requests");
        testProtectedResource(POST, "/api/expense/approve/" + REPORT_ID_1);
        testProtectedResource(POST, "/api/expense/reject/" + REPORT_ID_2);
    }

    @Test
    void testDiscovery() throws Exception {
        testConnectorDiscovery();
    }

    @Test
    void testRequestWithEmptyIssue() throws Exception {
        testRequestCards("emptyIssue.json", "emptyIssue.json", null);
    }

    @DisplayName("Missing parameter cases")
    @ParameterizedTest(name = "{index} ==> ''{0}''")
    @CsvSource({
            "emptyRequest.json, emptyRequest.json",
            "emptyToken.json, emptyToken.json"})
    void testRequestCardsWithMissingParameter(String requestFile, String responseFile) throws Exception {
        MockHttpServletRequestBuilder builder = requestCards("0_xxxxEKPk8cnYlWaos22OpPsLk=", requestFile);

        perform(builder)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(fromFile("connector/responses/" + responseFile)));
    }

    @Test
    void testGetImage() throws Exception {
        perform(get("/images/connector.png"))
                .andExpect(status().isOk())
                .andExpect(header().longValue(CONTENT_LENGTH, 9339))
                .andExpect(header().string(CONTENT_TYPE, IMAGE_PNG_VALUE))
                .andExpect((content().bytes(bytesFromFile("/static/images/connector.png"))));
    }

    @DisplayName("Card request success cases")
    @ParameterizedTest(name = "{index} ==> Language=''{0}''")
    @CsvSource({
            " , success.json",
            "xx, success_xx.json"})
    void testRequestCardsSuccess(String lang, String resFile) throws Exception {
        expect(REPORT_ID_1).andRespond(withSuccess(reportId1, APPLICATION_JSON));
        expect(REPORT_ID_2).andRespond(withSuccess(reportId2, APPLICATION_JSON));

        testRequestCards("request.json", resFile, lang);

        this.mockConcur.verify();
    }

    @Test
    void testApproveRequest() throws Exception {
        expect(REPORT_ID_1).andRespond(withSuccess(reportId1, APPLICATION_JSON));

        mockExpenseReport("/api/expense/approve/",
                REPORT_ID_1,
                approved,
                "https://implementation.concursolutions.com/api/expense/expensereport/v1.1/report/gWujNPAb67r9LjhqgN7BEYYaQOWzavXBtUP1sej$sXfPQ/WorkFlowAction");
    }

    @Test
    void testRejectRequest() throws Exception {
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
    void testEscapeHtmlTest() throws Exception {
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

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/expense/approve/",
            "/api/expense/reject/"})
    void testUnauthorizedRequest(String uri) throws Exception {
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

    @Test
    void testConcurRegex() throws Exception {
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
        for (final String input : regexInput.split("\\n")) {
            final Matcher matcher = pattern.matcher(input);
            while (matcher.find()) {
                result.add(matcher.group(1));
            }
        }
        assertThat(expectedList, equalTo(result));
    }

    private void testRequestCards(final String requestFile,
                                  final String responseFile,
                                  final String acceptLanguage) throws Exception {
        final MockHttpServletRequestBuilder builder = requestCards("0_xxxxEKPk8cnYlWaos22OpPsLk=", requestFile);
        if (StringUtils.isNotBlank(acceptLanguage)) {
            builder.header(ACCEPT_LANGUAGE, acceptLanguage);
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
