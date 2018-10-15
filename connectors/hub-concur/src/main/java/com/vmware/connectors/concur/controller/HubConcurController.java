/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur.controller;

import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.concur.service.HubConcurService;
import com.vmware.connectors.concur.util.HubConcurUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Locale;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class HubConcurController {

    private static final Logger logger = LoggerFactory.getLogger(HubConcurController.class);

    private static final String X_AUTH_HEADER = "X-Connector-Authorization";
    private static final String X_BASE_URL_HEADER = "X-Connector-Base-Url";

    private final HubConcurService service;

    @Autowired
    public HubConcurController(HubConcurService service) {
        this.service = service;
    }

    /**
     * API to request cards(pending approvals) for the user
     * @param username userName of the user logged in
     * @param vidmAuthHeader Bearer token for authorization
     * @param baseUrl cocnur base url
     * @param routingPrefix
     * @param locale
     * @return
     */
    @PostMapping(
            path = "/cards/requests",
            produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE
    )
    public Mono<Cards> getCards(
            @AuthenticationPrincipal String username,
            @RequestHeader(X_AUTH_HEADER) String vidmAuthHeader,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader("X-Routing-Prefix") String routingPrefix,
            Locale locale,
            HttpServletRequest request
    ) {
        logger.debug("getCards called: baseUrl={}, username={}", baseUrl, username);

        return service.fetchCards(baseUrl, locale, routingPrefix, request);
    }

    /**
     * API to approve the request provided through the report Id
     * @param baseUrl of the concur connector
     * @param id report Id
     * @param comment comments for the approval
     * @return success or error message
     * @throws IOException
     */
    @PostMapping(
            path = "/api/expense/{id}/approve",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<String> approveRequest(
            @AuthenticationPrincipal String username,
            @RequestHeader(X_AUTH_HEADER) String vidmAuthHeader,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @PathVariable("id") String id,
            @RequestParam(HubConcurUtil.COMMENT_KEY) String comment
    ) throws IOException {
        logger.debug("approveRequest called: baseUrl={}, username={}, id={}, comment={}", baseUrl, username, id, comment);

        return service.makeConcurRequest(comment, baseUrl, HubConcurUtil.APPROVE, id);
    }

    /**
     * API to decline the request provided through the report Id
     * @param baseUrl
     * @param id
     * @param reason
     * @return
     * @throws IOException
     */
    @PostMapping(
            path = "/api/expense/{id}/decline",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<String> declineRequest(
            @AuthenticationPrincipal String username,
            @RequestHeader(X_AUTH_HEADER) String vidmAuthHeader,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @PathVariable("id") String id,
            @RequestParam(HubConcurUtil.REASON_KEY) String reason
    ) throws IOException {
        logger.debug("declineRequest called: baseUrl={}, username={}, id={}, reason={}", baseUrl, username, id, reason);

        return service.makeConcurRequest(reason, baseUrl, HubConcurUtil.REJECT, id);
    }

}
