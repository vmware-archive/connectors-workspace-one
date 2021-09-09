/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static com.vmware.ws1connectors.workday.test.JsonUtils.convertFromJsonFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
@MockitoSettings
class ServiceTestsBase {
    static final String NO_BASE_URL = null;
    static final String NO_WORKDAY_TOKEN = null;
    static final String BASE_URL = "http://workday.com";
    static final String WORKDAY_TOKEN = "workdayToken";
    static final Duration DURATION_2_SECONDS = Duration.ofSeconds(2);

    @Mock
    protected ExchangeFunction mockExchangeFunc;
    protected WebClient restClient;

    protected void setupRestClient(final Object service, final String restClientFieldName) {
        restClient = WebClient.builder()
                .exchangeFunction(mockExchangeFunc)
                .build();
        setField(service, restClientFieldName, restClient);
    }

    protected void verifyWorkdayApiNeverInvoked() {
        verify(mockExchangeFunc, never()).exchange(any(ClientRequest.class));
    }

    protected void mockWorkdayApiResponse(final String responseBody) {
        final ClientResponse response = buildClientResponse(responseBody);
        mockExchangeFunctionWithResponse(response);
    }

    protected void mockWorkdayApiErrorResponse(final HttpStatus httpStatus) {
        final ClientResponse response = ClientResponse.create(httpStatus).build();
        mockExchangeFunctionWithResponse(response);
    }

    protected ClientResponse buildClientResponse(final String responseBody) {
        return ClientResponse.create(HttpStatus.OK)
                .body(responseBody)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private void mockExchangeFunctionWithResponse(ClientResponse response) {
        when(mockExchangeFunc.exchange(any())).thenReturn(Mono.just(response));
    }

    protected boolean isEquals(final Object actualObject, final String expectedObjectFile) {
        return isEquals(actualObject, convertFromJsonFile(expectedObjectFile, actualObject.getClass()));
    }

    protected boolean isEquals(final Object actualObject, final Object expectedObject) {
        return new EqualsBuilder()
                .setTestRecursive(true)
                .reflectionAppend(actualObject, expectedObject)
                .isEquals();
    }

}
