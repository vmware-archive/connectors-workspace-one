/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.coupa.controller;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.connectors.common.web.UserException;
import com.vmware.connectors.coupa.service.HubCoupaService;
import com.vmware.connectors.coupa.util.HubCoupaUtil;

import reactor.core.publisher.Mono;

@RestController
public class HubCoupaController {

	private static final Logger logger = LoggerFactory.getLogger(HubCoupaController.class);

	private static final String X_BASE_URL_HEADER = "X-Connector-Base-Url";
	private static final String X_AUTH_HEADER = "X-Connector-Authorization";

	@Value("classpath:static/discovery/metadata.json")
	private Resource metadata;

	private final HubCoupaService service;

	@Autowired
	public HubCoupaController(HubCoupaService service) {
		this.service = service;
	}

	@PostMapping(path = "/cards/requests", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<Cards> getCards(@RequestHeader(AUTHORIZATION) final String authorization,
			@RequestHeader(X_AUTH_HEADER) String vidmAuthHeader, @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
			@RequestHeader("X-Routing-Prefix") String routingPrefix, Locale locale, HttpServletRequest request)
			throws IOException {

		final String userEmail = AuthUtil.extractUserEmail(authorization);
		validateEmailAddress(userEmail);

		return service.getPendingApprovals(userEmail, baseUrl, routingPrefix, request, locale);
	}

	@GetMapping(path = "/api/approve/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> approveRequest(@RequestHeader(AUTHORIZATION) final String authorization,
			@RequestHeader(X_AUTH_HEADER) String vidmAuthHeader,
			@RequestHeader(name = X_BASE_URL_HEADER) String baseUrl, @RequestParam("comment") String comment,
			@PathVariable(name = "id") String id) throws IOException {

		final String userEmail = AuthUtil.extractUserEmail(authorization);
		validateEmailAddress(userEmail);

		return service.makeCoupaRequest(comment, baseUrl, HubCoupaUtil.APPROVE, id, userEmail);
	}

	@GetMapping(path = "/api/decline/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> declineRequest(@RequestHeader(AUTHORIZATION) final String authorization,
			@RequestHeader(X_AUTH_HEADER) String vidmAuthHeader,
			@RequestHeader(name = X_BASE_URL_HEADER) String baseUrl, @RequestParam("comment") String comment,
			@PathVariable(name = "id") String id) throws IOException {

		final String userEmail = AuthUtil.extractUserEmail(authorization);
		validateEmailAddress(userEmail);
		return service.makeCoupaRequest(comment, baseUrl, HubCoupaUtil.REJECT, id, userEmail);
	}

	private void validateEmailAddress(String userEmail) {
		// If email is not found in the token,throw Exception
		if (StringUtils.isBlank(userEmail)) {
			logger.error("User email  is empty in jwt access token.");
			throw new UserException("User Not Found");
		}
	}

}
