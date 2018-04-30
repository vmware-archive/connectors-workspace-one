/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.connectors.mock.MockWebServerWrapper;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Created by Rob Worsnop on 12/1/16.
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(JwtUtils.class)
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class ControllerTestsBase {

    private static final Logger logger = LoggerFactory.getLogger(ControllerTestsBase.class);

    @Autowired
    protected JwtUtils jwt;

    @Autowired
    protected ObjectMapper mapper;

    @Autowired
    protected WebTestClient webClient;

    private String auth;

    protected MockWebServerWrapper mockBackend;



    @BeforeEach
    void setup() throws Exception {
        auth = jwt.createAccessToken();
        mockBackend = new MockWebServerWrapper(new MockWebServer());
    }

    @AfterEach
    void shutdown() throws IOException {
        mockBackend.verify();
        mockBackend.shutdown();
    }

    protected String accessToken() {
        return auth;
    }

    protected void testProtectedResource(HttpMethod method, String uri) throws Exception {
        // Try without authorization; should never work
        webClient.method(method)
                .uri(uri)
                .exchange()
                .expectStatus().isUnauthorized();

        // Try with expired token
        webClient.method(method)
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + jwt.createAccessToken(Instant.now()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    public static String fromFile(String fileName) throws IOException {
        try (InputStream stream = new ClassPathResource(fileName).getInputStream()) {
            return IOUtils.toString(stream, Charset.defaultCharset());
        }
    }

    public static byte[] bytesFromFile(String fileName) {
        try (InputStream stream = new ClassPathResource(fileName).getInputStream()) {
            return IOUtils.toByteArray(stream);
        } catch (IOException e) {
            throw new RuntimeException(e); //NOPMD allows method to be called from lambda
        }
    }

    protected void testConnectorDiscovery() throws IOException {
        webClient.get()
                .uri("/")
                .headers(ControllerTestsBase::headers)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody().json(fromFile("/connector/responses/discovery.json"));

        webClient.get()
                .uri("/discovery/metadata.json")
                .headers(ControllerTestsBase::headers)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody().json(fromFile("/static/discovery/metadata.json"));
    }

    protected static void headers(HttpHeaders headers) {
        headers.add("x-forwarded-host", "my-connector");
        headers.add("x-forwarded-proto", "https");
        headers.add("x-forwarded-port", "443");
    }

    protected void testRegex(String tokenProperty, String emailInput, List<String> expected) throws Exception {
        byte[] body = webClient.get()
                .uri("/discovery/metadata.json")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .returnResult(byte[].class).getResponseBodyContent();

            Map<String, Object> results = mapper.readValue(body, Map.class);
            Map<String, Object> fields = (Map<String, Object>) results.get("fields");
            Map<String, Object> tokenDefinition = (Map<String, Object>) fields.get(tokenProperty);
            String regex = (String) tokenDefinition.get("regex");
            Integer captureGroup = (Integer) tokenDefinition.get("capture_group");
            verifyRegex(regex, captureGroup, emailInput, expected);
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
