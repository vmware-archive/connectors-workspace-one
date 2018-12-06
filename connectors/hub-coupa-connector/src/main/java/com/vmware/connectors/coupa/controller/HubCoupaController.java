package com.vmware.connectors.coupa.controller;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.coupa.service.HubCoupaService;
import com.vmware.connectors.coupa.util.HubCoupaUtil;

import reactor.core.publisher.Mono;

@RestController
public class HubCoupaController {

	private static final Logger logger = LoggerFactory.getLogger(HubCoupaController.class);

	private static final String X_BASE_URL_HEADER = "X-Connector-Base-Url";

	@Value("classpath:static/discovery/metadata.json")
	private Resource metadata;

	private final HubCoupaService service;

	@Autowired
	public HubCoupaController(HubCoupaService service) {
		this.service = service;
	}

	@PostMapping(path = "/cards/requests", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<Cards> getCards(@RequestHeader(AUTHORIZATION) String vidmAuthHeader,
			@RequestHeader(name = X_BASE_URL_HEADER) String baseUrl,
			@RequestHeader(name = "X-Routing-Prefix") String routingPrefix, Locale locale,
			@Valid @RequestBody CardRequest cardRequest, HttpServletRequest request) {

		String user = cardRequest.getTokenSingleValue("user_email");
		logger.debug("User email: {} for coupa server: {} ", user, baseUrl);

		if (StringUtils.isEmpty(user)) {
			logger.warn("user email is blank for url: {}", baseUrl);

		}

		return service.getPendingApprovals(user, baseUrl, routingPrefix, request, locale);
	}

	@GetMapping(path = "/api/approve/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> approveRequest(@RequestHeader(AUTHORIZATION) String vidmAuthHeader,
			@RequestHeader(name = X_BASE_URL_HEADER) String baseUrl,
			@RequestHeader(name = "X-Routing-Prefix") String routingPrefix, @RequestParam("comment") String comment,
			@PathVariable(name = "id") String id) throws IOException {

		return service.makeCoupaRequest(comment, baseUrl, HubCoupaUtil.APPROVE, id);
	}

	@GetMapping(path = "/api/decline/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> declineRequest(@RequestHeader(AUTHORIZATION) String vidmAuthHeader,
			@RequestHeader(name = X_BASE_URL_HEADER) String baseUrl,
			@RequestHeader(name = "X-Routing-Prefix") String routingPrefix, @RequestParam("comment") String comment,
			@PathVariable(name = "id") String id) throws IOException {

		return service.makeCoupaRequest(comment, baseUrl, HubCoupaUtil.REJECT, id);
	}

}
