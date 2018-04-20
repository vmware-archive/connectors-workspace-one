/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.aws.cert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.connectors.mock.MockClientHttpConnector;
import com.vmware.connectors.mock.RequestHandlerHolder;
import com.vmware.connectors.test.JwtUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientCodecCustomizer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by Rob Worsnop on 12/1/16.
 */
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@SpringBootTest
@Import(JwtUtils.class)
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class ControllerTestsBase {

    private static final Logger logger = LoggerFactory.getLogger(com.vmware.connectors.test.ControllerTestsBase.class);

    @Autowired
    protected RequestHandlerHolder requestHandlerHolder;

    @Autowired
    protected JwtUtils jwt;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    private String auth;

    @TestConfiguration
    static class ControllerTestConfiguration {
        @Bean
        public RequestHandlerHolder requestHandler() {
            return new RequestHandlerHolder();
        }

        @Bean
        public WebClient.Builder webClientBuilder(WebClientCodecCustomizer codecCustomizer) {
            WebClient.Builder builder = WebClient.builder();
            codecCustomizer.customize(builder);
            builder.clientConnector(new MockClientHttpConnector(requestHandler()));
            return builder;
        }
    }

    protected void setup() throws Exception {
        auth = jwt.createAccessToken();
    }

    protected String accessToken() {
        return auth;
    }

    protected void testProtectedResource(HttpMethod method, String uri) throws Exception {
        // Try without authorization; should never work
        perform(request(method, uri))
                .andExpect(status().isUnauthorized());

        // Try with expired token
        perform(request(method, uri).with(token(jwt.createAccessToken(Instant.now()))))
                .andExpect(status().isUnauthorized());
    }

    protected ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        ResultActions resultActions = mvc.perform(builder
                .header("x-forwarded-host", "my-connector")
                .header("x-forwarded-proto", "https")
                .header("x-forwarded-port", "443"));
        if (resultActions.andReturn().getRequest().isAsyncStarted()) {
            return mvc.perform(asyncDispatch(resultActions
                    .andExpect(MockMvcResultMatchers.request().asyncResult(anything()))
                    .andReturn()));
        }
        return resultActions;
    }

    protected static RequestPostProcessor token(String accessToken) {
        return request -> {
            request.addHeader(AUTHORIZATION, "Bearer " + accessToken);
            return request;
        };
    }

    public static String fromFile(String fileName) throws IOException {
        try (InputStream stream = new ClassPathResource(fileName).getInputStream()) {
            return IOUtils.toString(stream, Charset.defaultCharset());
        }
    }

    protected void testConnectorDiscovery() throws Exception {
        perform(request(GET, "/"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fromFile("/connector/responses/discovery.json")));
        perform(request(GET, "/discovery/metadata.json"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fromFile("/static/discovery/metadata.json")));
    }

    protected void testRegex(String tokenProperty, String emailInput, List<String> expected) throws Exception {
        mvc.perform(
                get("/discovery/metadata.json")
                        .with(token(accessToken()))
                        .accept(APPLICATION_JSON)
        ).andExpect(mvcResult -> {
            String json = mvcResult.getResponse().getContentAsString();
            Map<String, Object> results = mapper.readValue(json, Map.class);
            Map<String, Object> fields = (Map<String, Object>) results.get("fields");
            Map<String, Object> tokenDefinition = (Map<String, Object>) fields.get(tokenProperty);
            String regex = (String) tokenDefinition.get("regex");
            Integer captureGroup = (Integer) tokenDefinition.get("capture_group");
            verifyRegex(regex, captureGroup, emailInput, expected);
        });
    }

    private void verifyRegex(String regex, Integer captureGroup, String emailInput, List<String> expected) throws Exception {
        List<String> results = new RegexMatcher().getMatches(regex, emailInput, Optional.ofNullable(captureGroup).orElse(0));
        assertThat(results, equalTo(expected));
    }

    /*
     * Code ported from the Android project's code to match more closely what the client is doing:
     * - https://stash.air-watch.com/projects/UFO/repos/android-roswell-framework/browse/roswellframework/src/main/java/com/vmware/roswell/framework/etc/RegexMatcher.java?at=403dfa349a17901ba3a888eb2e98ab14ddae5825#39
     * - https://stash.air-watch.com/projects/UFO/repos/android-roswell-framework/browse/roswellframework/src/main/java/com/vmware/roswell/framework/json/HCSConnectorDeserializer.java?at=403dfa349a17901ba3a888eb2e98ab14ddae5825#114
     */
    private static class RegexMatcher {
        List<String> getMatches(String regex, String text, int captureGroupIndex) {
            List<String> allMatches = new ArrayList<>();

            for (String line : text.split("\\n")) {
                Matcher m = Pattern.compile(regex).matcher(line);
                int totalGroupCount = m.groupCount() + 1; // +1 because group zero (the whole regex) isn't included in groupCount()
                if (captureGroupIndex >= 0 && captureGroupIndex < totalGroupCount) {

                    while (m.find()) {
                        allMatches.add(m.group(captureGroupIndex));
                    }
                } else {
                    logger.warn(
                            "Connector has a regex field with capture_group_index ({}) greater than the number of groups ({}) in the regex << {} >>",
                            captureGroupIndex, totalGroupCount, regex
                    );
                    break;
                }
            }

            return allMatches;
        }
    }

}
