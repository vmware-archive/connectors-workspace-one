/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by Rob Worsnop on 12/1/16.
 */
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@SpringBootTest
@Import(JwtUtils.class)
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class ControllerTestsBase {

    @Autowired
    protected JwtUtils jwt;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    private String accessToken;

    protected void setup() throws Exception {

        accessToken = jwt.createAccessToken();

    }

    protected String getAccessToken() {
        return accessToken;
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

    public static byte[] bytesFromFile(String fileName) throws IOException {
        try (InputStream stream = new ClassPathResource(fileName).getInputStream()) {
            return IOUtils.toByteArray(stream);
        }
    }

    protected void testConnectorDiscovery() throws Exception {
        perform(request(GET, "/"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fromFile("/connector/responses/discovery.json")));
        perform(request(GET, "/discovery/metadata.hal"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fromFile("/static/discovery/metadata.hal")));
    }

    protected void testRegex(String tokenProperty, String emailInput, List<String> expected) throws Exception {
        mvc.perform(
                get("/discovery/metadata.hal")
                        .with(token(getAccessToken()))
                        .accept(APPLICATION_JSON)
        ).andExpect(mvcResult -> {
            String json = mvcResult.getResponse().getContentAsString();
            Map<String, Object> results = mapper.readValue(json, Map.class);
            Map<String, Object> fields = (Map<String, Object>) results.get("fields");
            Map<String, Object> tokenDefinition = (Map<String, Object>) fields.get(tokenProperty);
            String regex = (String) tokenDefinition.get("regex");
            verifyRegex(regex, emailInput, expected);
        });
    }

    private void verifyRegex(String regex, String emailInput, List<String> expected) throws Exception {
        Pattern pattern = Pattern.compile(regex);

        List<String> results = new ArrayList<>();
        for (String line : emailInput.split("\\n")) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                results.add(matcher.group(1));
            }
        }

        assertThat(results, equalTo(expected));
    }

}
