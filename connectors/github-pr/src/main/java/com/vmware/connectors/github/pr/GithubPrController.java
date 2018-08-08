/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.github.pr;

import com.google.common.collect.ImmutableMap;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.CommonUtils;
import com.vmware.connectors.common.utils.Reactive;
import com.vmware.connectors.github.pr.v3.PullRequest;
import com.vmware.connectors.github.pr.v3.Review;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestController
public class GithubPrController {

    private static final Logger logger = LoggerFactory.getLogger(GithubPrController.class);

    private static final String AUTH_HEADER = "X-Connector-Authorization";
    private static final String BASE_URL_HEADER = "X-Connector-Base-Url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";

    private static final String OPEN_STATE = "open";

    private static final String COMMENT_PARAM_KEY = "message";

    private static final int URI_SEGMENT_SIZE = 4;

    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public GithubPrController(
            WebClient rest,
            CardTextAccessor cardTextAccessor
    ) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
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

        Stream<PullRequestId> pullRequestIds = cardRequest.getTokens("pull_request_urls")
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()) // squash duplicates
                .stream()
                .sorted()
                .map(this::parseUri)
                .filter(Objects::nonNull)
                .filter(this::validHost)
                .map(this::getPullRequestId)
                .filter(Objects::nonNull);

        return Flux.fromStream(pullRequestIds)
                .flatMap(pullRequestId -> fetchPullRequest(baseUrl, pullRequestId, auth))
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
        return "github.com".equalsIgnoreCase(uriComponents.getHost());
    }

    private PullRequestId getPullRequestId(
            UriComponents uri
    ) {
        if (uri.getPathSegments().size() != URI_SEGMENT_SIZE) {
            return null;
        }
        PullRequestId pullRequestId = new PullRequestId();
        pullRequestId.setOwner(uri.getPathSegments().get(0));
        pullRequestId.setRepo(uri.getPathSegments().get(1));
        pullRequestId.setNumber(uri.getPathSegments().get(3));
        return pullRequestId;
    }

    private Mono<Pair<PullRequestId, PullRequest>> fetchPullRequest(
            String baseUrl,
            PullRequestId pullRequestId,
            String auth
    ) {
        logger.trace("fetchPullRequest called: baseUrl={}, id={}", baseUrl, pullRequestId);
        return rest.get()
                .uri(makeGithubUri(baseUrl, pullRequestId))
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(PullRequest.class)
                .onErrorResume(Reactive::skipOnNotFound)
                .map(pullRequest -> Pair.of(pullRequestId, pullRequest));
    }

    private String makeGithubUri(
            String baseUrl,
            PullRequestId pullRequestId
    ) {
        return UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/repos/{owner}/{repo}/pulls/{number}")
                .buildAndExpand(
                        ImmutableMap.of(
                                "owner", pullRequestId.getOwner(),
                                "repo", pullRequestId.getRepo(),
                                "number", pullRequestId.getNumber()
                        )
                )
                .encode()
                .toUri()
                .toString();
    }

    private Card makeCard(
            String routingPrefix,
            Pair<PullRequestId, PullRequest> info,
            Locale locale,
            HttpServletRequest request
    ) {
        logger.trace("makeCard called: routingPrefix={}, info={}", routingPrefix, info);

        PullRequestId pullRequestId = info.getLeft();
        PullRequest pullRequest = info.getRight();
        boolean isOpen = OPEN_STATE.equalsIgnoreCase(pullRequest.getState());

        Card.Builder card = new Card.Builder()
                .setName("GithubPr") // TODO - remove this in APF-536
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(
                        cardTextAccessor.getHeader(
                                locale,
                                pullRequestId.getNumber(),
                                pullRequest.getUser().getLogin()
                        ),
                        cardTextAccessor.getMessage(
                                "subtitle",
                                locale,
                                pullRequestId.getOwner(),
                                pullRequestId.getRepo()
                        )
                );

        // Set image url.
        CommonUtils.buildConnectorImageUrl(card, request);

        addApproveAction(card, routingPrefix, pullRequestId, isOpen, locale);
        addCommentAction(card, routingPrefix, pullRequestId, locale);

        return card.build();
    }

    private void addApproveAction(
            Card.Builder card,
            String routingPrefix,
            PullRequestId pullRequestId,
            boolean isOpen,
            Locale locale
    ) {
        if (isOpen) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("approve", locale))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("approve", locale))
                            .setActionKey(CardActionKey.DIRECT)
                            .setUrl(getActionUrl(routingPrefix, pullRequestId, "approve"))
                            .setType(HttpMethod.POST)
                            .build()
            );
        }
    }

    private void addCommentAction(
            Card.Builder card,
            String routingPrefix,
            PullRequestId pullRequestId,
            Locale locale
    ) {
        card.addAction(
                new CardAction.Builder()
                        .setLabel(cardTextAccessor.getActionLabel("comment", locale))
                        .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("comment", locale))
                        .setActionKey(CardActionKey.USER_INPUT)
                        .setUrl(getActionUrl(routingPrefix, pullRequestId, "comment"))
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

    private String getActionUrl(final String routingPrefix,
                                final PullRequestId pullRequestId,
                                final String action) {
        return routingPrefix + "api/v1/" + pullRequestId.getOwner() + "/" + pullRequestId.getRepo() + "/" + pullRequestId.getNumber() + "/" + action;
    }

    @PostMapping(
            path = "/api/v1/{owner}/{repo}/{number}/approve",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<String> approve(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            PullRequestId pullRequestId
    ) {
        logger.trace(
                "approve called: baseUrl={}, id={}",
                baseUrl,
                pullRequestId
        );

        return postReview(
                pullRequestId,
                Review.approve(),
                auth,
                baseUrl
        );
    }

    @PostMapping(
            path = "/api/v1/{owner}/{repo}/{number}/comment",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<String> comment(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            PullRequestId pullRequestId,
            @RequestParam(COMMENT_PARAM_KEY) String comment
    ) {
        logger.trace(
                "comment called: baseUrl={}, id={}, comment={}",
                baseUrl,
                pullRequestId,
                comment
        );

        return postReview(
                pullRequestId,
                Review.comment(comment),
                auth,
                baseUrl
        );
    }

    private Mono<String> postReview(
            PullRequestId pullRequestId,
            Review review,
            String auth,
            String baseUrl
    ) {
        return rest.post()
                .uri(makeGithubUri(baseUrl, pullRequestId) + "/reviews")
                .header(AUTHORIZATION, auth)
                .contentType(APPLICATION_JSON)
                .syncBody(review)
                .retrieve()
                .bodyToMono(String.class);
    }

}
