/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.web;

import com.vmware.connectors.test.ControllerTestsBase;
import org.apache.commons.lang3.StringUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;

import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.COMMUNITY_COMMON_API_V1;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASKS_SUMMARY;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASKS_VIEW_QUERY_PARAM_NAME;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.WORKERS_INBOX_TASKS_API;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.ROUTING_PREFIX_HEADER;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class ControllerTestUtils extends ControllerTestsBase {

    protected static final String BEARER = "Bearer ";
    protected static final String ROUTING_PREFIX = "https://dev.hero.example.com/connectors/id/";
    protected static final String WORKDAY_AUTH_TOKEN = BEARER + "valid-auth-token";
    private static final String TENANT_NAME = "vmware_gms";

    private WebTestClient.ResponseSpec doPost(final String path, final String authToken, final String language, String requestBodyFileName, final boolean isPreHire)
            throws IOException {
        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(path)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header(X_BASE_URL_HEADER, mockBackend.url(StringUtils.EMPTY))
                .header(ROUTING_PREFIX_HEADER, ROUTING_PREFIX)
                .header(ACCEPT_LANGUAGE, Locale.US.toLanguageTag())
                .bodyValue(fromFile(requestBodyFileName));
        if (isPreHire) {
            spec.headers(headers -> preHireHeaders(headers, path, true));
        } else {
            spec.headers(headers -> headers(headers, path));
        }
        if (StringUtils.isNotBlank(authToken)) {
            spec = spec.header(X_AUTH_HEADER, authToken);
        }
        if (StringUtils.isNotBlank(language)) {
            spec = spec.header(ACCEPT_LANGUAGE, language);
        }
        return spec.exchange();
    }

    protected WebTestClient.ResponseSpec requestCards(final String authToken, String path, String requestBodyFileName, final boolean isPreHire)
            throws IOException {
        return requestCards(authToken, null, path, requestBodyFileName, isPreHire);
    }

    protected void mockWorkdayApiResponse(String workdayApi, String responseFile) throws IOException {
        mockBackend.expect(requestTo(workdayApi))
                .andExpect(header(AUTHORIZATION, WORKDAY_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile(responseFile).replace("{MOCK_BACKEND}/", mockBackend.url("/")), APPLICATION_JSON));
    }

    private WebTestClient.ResponseSpec requestCards(final String authToken, final String language, final String path, String requestBodyFileName, final boolean isPreHire)
            throws IOException {
        return doPost(path, authToken, language, requestBodyFileName, isPreHire);
    }

    protected String getInboxTasksUri() {
        return UriComponentsBuilder.fromPath(COMMUNITY_COMMON_API_V1 + TENANT_NAME + WORKERS_INBOX_TASKS_API)
                .queryParam(INBOX_TASKS_VIEW_QUERY_PARAM_NAME, INBOX_TASKS_SUMMARY)
                .build()
                .toUriString();
    }
}
