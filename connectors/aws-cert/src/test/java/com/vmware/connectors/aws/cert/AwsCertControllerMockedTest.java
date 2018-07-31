/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.aws.cert;

import com.google.common.collect.ImmutableSet;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.test.ControllerTestsBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test that approval URLs are validated correctly.
 * This can't be done with the other test because we use OkHttp as the backend, whose hostname is localhost.
 */
@ExtendWith(MockitoExtension.class)
public class AwsCertControllerMockedTest {

    private final static String APPROVAL_URL_WRONG_HOST_1 = "https://justwrong/approvals?code=test-auth-code-2&context=test-context-1";
    private final static String APPROVAL_URL_WRONG_HOST_2 = "https://test-aws-region-2.BADcertificates.Fake-Amazon.com/approvals?code=test-auth-code-2&context=test-context-2";
    private final static String APPROVAL_URL_WRONG_PATH = "https://test-aws-region.certificates.Fake-Amazon.com/wrong";
    private final static String APPROVAL_URL_CORRECT = "https://test-aws-region.certificates.Fake-Amazon.com/approvals?code=test-auth-code-1&context=test-context-1";
    private final static String APPROVAL_URL_CORRECT_EXACT_HOST = "https://certificates.Fake-Amazon.com/approvals?code=test-auth-code-1&context=test-context-1";

    private AwsCertController controller;

    @Mock
    private ClientHttpConnector mockClientHttpConnector;

    @BeforeEach
    void createController() throws IOException {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setBasename("cards/text");

        Resource metadataJsonResource = new ClassPathResource("discovery/metadata.json");

        controller = new AwsCertController(
                "certificates.Fake-Amazon.com",
                "/approvals",
                metadataJsonResource,
                WebClient.builder().clientConnector(mockClientHttpConnector).build(),
                new CardTextAccessor(messageSource));
    }

    @ParameterizedTest
    @CsvSource({
            APPROVAL_URL_WRONG_HOST_1 + ", 400",
            APPROVAL_URL_WRONG_HOST_2 + ", 400",
            APPROVAL_URL_WRONG_PATH + ", 400",
            APPROVAL_URL_CORRECT + ", 200",
            APPROVAL_URL_CORRECT_EXACT_HOST + ", 200"
    })
    void testApprove(String approvalUrl, int status) {
        if (status == HttpStatus.OK.value()) {
            when(mockClientHttpConnector.connect(eq(HttpMethod.POST), eq(URI.create(approvalUrl)), any()))
                    .thenReturn(Mono.just(new MockClientHttpResponse(HttpStatus.OK)));
        }

        Map<String, String> params = Collections.singletonMap("hero_aws_cert_approval_url", approvalUrl);
        ResponseEntity<String> response = controller.approve(params).block();
        assertThat(response.getStatusCode().value(), equalTo(status));

        if (status == HttpStatus.OK.value()) {
            verify(mockClientHttpConnector).connect(eq(HttpMethod.POST), eq(URI.create(approvalUrl)), any());
        }
    }

    @Test
    void testCardsRequest() throws IOException {

        Set<String> approvalUrls = ImmutableSet.<String>builder()
                .add(APPROVAL_URL_WRONG_HOST_1)
                .add(APPROVAL_URL_WRONG_HOST_2)
                .add(APPROVAL_URL_WRONG_PATH)
                .add(APPROVAL_URL_CORRECT)
                .add(APPROVAL_URL_CORRECT_EXACT_HOST)
                .build();
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(HttpStatus.OK);
        mockResponse.setBody(ControllerTestsBase.fromFile("awscert/fake/approval-page-1.html"));

        when(mockClientHttpConnector.connect(eq(HttpMethod.GET),
                or(eq(URI.create(APPROVAL_URL_CORRECT)),
                        eq(URI.create(APPROVAL_URL_CORRECT_EXACT_HOST))),
                any()))
                .thenReturn(Mono.just(mockResponse));

        Map<String, Set<String>> tokens = Collections.singletonMap("approval_urls", approvalUrls);
        CardRequest cardRequest = new CardRequest(tokens);
        HttpServletRequest servletRequest = new MockHttpServletRequest();

        Cards cards = controller.getCards("https://hero/connectors/aws", null, cardRequest, servletRequest).block();

        assertThat(cards.getCards(), is(iterableWithSize(2)));

        verify(mockClientHttpConnector).connect(eq(HttpMethod.GET), eq(URI.create(APPROVAL_URL_CORRECT)), any());
        verify(mockClientHttpConnector).connect(eq(HttpMethod.GET), eq(URI.create(APPROVAL_URL_CORRECT_EXACT_HOST)), any());
    }

}
