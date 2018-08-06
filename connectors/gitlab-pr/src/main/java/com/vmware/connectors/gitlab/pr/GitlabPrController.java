/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.gitlab.pr;

import com.google.common.collect.ImmutableMap;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.CommonUtils;
import com.vmware.connectors.common.utils.Reactive;
import com.vmware.connectors.gitlab.pr.v4.MergeRequest;
import com.vmware.connectors.gitlab.pr.v4.MergeRequestActionConstants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestController
public class GitlabPrController {

    private static final Logger logger = LoggerFactory.getLogger(GitlabPrController.class);

    private static final String AUTH_HEADER = "X-Connector-Authorization";
    private static final String BASE_URL_HEADER = "X-Connector-Base-Url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";

    private static final String COMMENT_PARAM_KEY = "message";
    private static final String SHA_PARAM_KEY = "sha";

    // Number of path segments expected in our pull request urls
    private static final int NUM_PR_PATH_SEGMENTS = 4;

    // Merge Request path name expected
    private static final String MERGE_REQUESTS = "merge_requests";

    private final boolean isEnterpriseEdition;
    private final WebClient rest;
    private final String metadata;
    private final CardTextAccessor cardTextAccessor;
    private final long maxAge;
    private final TimeUnit unit;

    @Autowired
    public GitlabPrController(
            @Value("${gitlab.connector.enterprise:false}") boolean isEnterpriseEdition,
            WebClient rest,
            CardTextAccessor cardTextAccessor,
            @Value("classpath:static/discovery/metadata.json") Resource metadataJsonResource,
            @Value("${rootDiscovery.cacheControl.maxAge:1}") long maxAge,
            @Value("${rootDiscovery.cacheControl.unit:HOURS}") TimeUnit unit
    ) throws IOException {
        this.isEnterpriseEdition = isEnterpriseEdition;
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.metadata = IOUtils.toString(metadataJsonResource.getInputStream(), Charset.defaultCharset());
        this.maxAge = maxAge;
        this.unit = unit;
    }

    @GetMapping(path = "/")
    public ResponseEntity<String> getMetadata(HttpServletRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(maxAge, unit))
                .body(this.metadata.replace("${CONNECTOR_HOST}", CommonUtils.buildConnectorUrl(request, null)));
    }

    @PostMapping(
            path = "/cards/requests",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<Cards> getCards(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            Locale locale,
            @Valid @RequestBody CardRequest cardRequest,
            final HttpServletRequest request
    ) {
        logger.trace("getCards called: baseUrl={}, routingPrefix={}, request={}", baseUrl, routingPrefix, cardRequest);

        Stream<MergeRequestId> mergeRequestIds = cardRequest.getTokens("merge_request_urls")
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()) // squash duplicates
                .stream()
                .sorted()
                .map(this::parseUri)
                .filter(Objects::nonNull)
                .filter(this::validHost)
                .map(this::getMergeRequestId)
                .filter(Objects::nonNull);

       return Flux.fromStream(mergeRequestIds)
                .flatMap(mergeRequestId -> fetchMergeRequest(baseUrl, mergeRequestId, auth))
                .map(pair -> makeCard(routingPrefix, pair, locale, request))
                .reduce(
                        new Cards(),
                        (cards, card) -> {
                            cards.getCards().add(card);
                            return cards;
                        }
                )
               .defaultIfEmpty(new Cards())
               .subscriberContext(Reactive.setupContext());
    }

    private UriComponents parseUri(
            String url
    ) {
        try {
            return UriComponentsBuilder
                    .fromHttpUrl(url)
                    .build();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean validHost(
            UriComponents uriComponents
    ) {
        return "gitlab.com".equalsIgnoreCase(uriComponents.getHost());
    }

    private MergeRequestId getMergeRequestId(
            UriComponents uri
    ) {
        if (uri.getPathSegments().size() != NUM_PR_PATH_SEGMENTS) {
            return null;
        }
        if (!MERGE_REQUESTS.equals(uri.getPathSegments().get(2))) {
            return null;
        }
        MergeRequestId mergeRequestId = new MergeRequestId();
        mergeRequestId.setNamespace(uri.getPathSegments().get(0));
        mergeRequestId.setProjectName(uri.getPathSegments().get(1));
        mergeRequestId.setNumber(uri.getPathSegments().get(3));
        return mergeRequestId;
    }

    private Mono<Pair<MergeRequestId, MergeRequest>> fetchMergeRequest(
            String baseUrl,
            MergeRequestId mergeRequestId,
            String auth
    ) {
        logger.trace("fetchMergeRequest called: baseUrl={}, id={}", baseUrl, mergeRequestId);

        return rest.get()
                .uri(makeGitlabUri(baseUrl, mergeRequestId))
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(MergeRequest.class)
                .onErrorResume(Reactive::skipOnNotFound)
                .map(mergeRequest -> Pair.of(mergeRequestId, mergeRequest));
   }

    private URI makeGitlabUri(
            String baseUrl,
            MergeRequestId mergeRequestId
    ) {
        return makeGitlabUri(baseUrl, mergeRequestId, "");
    }

    private URI makeGitlabUri(
            String baseUrl,
            MergeRequestId mergeRequestId,
            String action
    ) {
        /*
         * I can't really leverage UriComponentsBuilder here due to gitlab's
         * "id" being "namespace/projectName" uri-encoded.  This makes it
         * problematic to use UriComponentsBuilder because I can't get it to
         * encode on the buildAndExpand, but not double encode if I
         * buildAndExpand with the %2F specified in MergeRequestId's
         * getProjectId().
         */
        try {
            return new URI(
                    // Since we're not using UriComponentsBuilder, ensure we're not repeating "/" after base URL.
                    baseUrl.replaceAll("/$", "") + "/api/v4/projects/" + mergeRequestId.getProjectId()
                            + "/" + MERGE_REQUESTS + "/" + mergeRequestId.getNumber()
                            + action
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException("Malformed URI formed from: " + mergeRequestId, e); // NOPMD
        }
    }

    private Card makeCard(
            String routingPrefix,
            Pair<MergeRequestId, MergeRequest> info,
            Locale locale,
            HttpServletRequest request
    ) {
        logger.trace("makeCard called: routingPrefix={}, info={}", routingPrefix, info);

        MergeRequestId mergeRequestId = info.getLeft();
        MergeRequest mergeRequest = info.getRight();

        Card.Builder card = new Card.Builder()
                .setName("GitlabPr") // TODO - remove this in APF-536
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(
                        cardTextAccessor.getHeader(
                                locale,
                                mergeRequestId.getNumber(),
                                mergeRequest.getAuthor().getUsername()
                        ),
                        cardTextAccessor.getMessage(
                                "subtitle",
                                locale,
                                mergeRequestId.getNamespace(),
                                mergeRequestId.getProjectName()
                        )
                );

        addApproveAction(card, routingPrefix, mergeRequestId, mergeRequest, locale);
        addCommentAction(card, routingPrefix, mergeRequestId, locale);

        // Set image url.
        CommonUtils.buildConnectorImageUrl(card, request);

        return card.build();
    }

    private void addApproveAction(
            Card.Builder card,
            String routingPrefix,
            MergeRequestId mergeRequestId,
            MergeRequest mergeRequest,
            Locale locale
    ) {
        if (mergeRequest.getState().isOpen() && isEnterpriseEdition) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("approve", locale))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("approve", locale))
                            .setActionKey(CardActionKey.DIRECT)
                            .setUrl(getActionUrl(routingPrefix, mergeRequestId, "approve"))
                            .addRequestParam(SHA_PARAM_KEY, mergeRequest.getSha())
                            .setType(HttpMethod.POST)
                            .build()
            );
        }
    }

    private void addCommentAction(
            Card.Builder card,
            String routingPrefix,
            MergeRequestId mergeRequestId,
            Locale locale
    ) {
        card.addAction(
                new CardAction.Builder()
                        .setLabel(cardTextAccessor.getActionLabel("comment", locale))
                        .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("comment", locale))
                        .setActionKey(CardActionKey.USER_INPUT)
                        .setUrl(getActionUrl(routingPrefix, mergeRequestId, "comment"))
                        .addUserInputField(
                                new CardActionInputField.Builder()
                                        .setId(COMMENT_PARAM_KEY)
                                        .setFormat("textarea")
                                        .setLabel(cardTextAccessor.getActionLabel("comment.comment", locale))
                                        .setMinLength(1)
                                        .build()
                        )
                        .setType(HttpMethod.POST)
                        .build()
        );
    }

    private String getActionUrl(String routingPrefix, MergeRequestId mergeRequestId, String action) {
        return String.format(
                "%sapi/v1/%s/%s/%s/%s",
                routingPrefix,
                mergeRequestId.getNamespace(),
                mergeRequestId.getProjectName(),
                mergeRequestId.getNumber(),
                action
        );
    }

    @PostMapping(
            path = "/api/v1/{namespace}/{projectName}/{number}/comment",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<String> comment(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            MergeRequestId mergeRequestId,
            @RequestParam(COMMENT_PARAM_KEY) String comment
    ) {
        logger.trace(
                "comment called: baseUrl={}, id={}, comment={}",
                baseUrl,
                mergeRequestId,
                comment
        );

        return postNote(auth, baseUrl, mergeRequestId, comment);
    }

    private Mono<String> postNote(
            String auth,
            String baseUrl,
            MergeRequestId mergeRequestId,
            String comment
    ) {
        Map<String, Object> body = ImmutableMap.of(
                MergeRequestActionConstants.Properties.BODY, comment
        );

        return actionRequest(auth, baseUrl, mergeRequestId, "/notes", HttpMethod.POST, body);
    }

    private Mono<String> actionRequest(
            String auth,
            String baseUrl,
            MergeRequestId mergeRequestId,
            String action,
            HttpMethod method,
            Map<String, Object> body
    ) {
        return rest.method(method)
                .uri(makeGitlabUri(baseUrl, mergeRequestId, action))
                .header(AUTHORIZATION, auth)
                .contentType(APPLICATION_JSON)
                .syncBody(body)
                .retrieve()
                .bodyToMono(String.class);
    }

    @PostMapping(
            path = "/api/v1/{namespace}/{projectName}/{number}/approve",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<String> approve(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            MergeRequestId mergeRequestId,
            @RequestParam(SHA_PARAM_KEY) String sha
    ) {
        logger.trace(
                "approve called: baseUrl={}, id={}, sha={}",
                baseUrl,
                mergeRequestId,
                sha
        );

        Map<String, Object> body = ImmutableMap.of(
                MergeRequestActionConstants.Properties.SHA, sha
        );

        return actionRequest(auth, baseUrl, mergeRequestId, "/approve", HttpMethod.POST, body);
    }

}
