/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.github.pr;

import com.google.common.collect.ImmutableMap;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.Reactive;
import com.vmware.connectors.github.pr.v3.PullRequest;
import com.vmware.connectors.github.pr.v3.Review;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestController
public class GithubPrController {

    private static final Logger logger = LoggerFactory.getLogger(GithubPrController.class);

    private static final String AUTH_HEADER = "x-github-pr-authorization";
    private static final String BASE_URL_HEADER = "x-github-pr-base-url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";

    private static final String OPEN_STATE = "open";

    private static final String CLOSE_REASON_PARAM_KEY = "reason";
    private static final String COMMENT_PARAM_KEY = "message";
    private static final String REQUEST_PARAM_KEY = "request";
    private static final String SHA_PARAM_KEY = "sha";

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
            @RequestHeader(value = ACCEPT_LANGUAGE, required = false) Locale locale,
            @Valid @RequestBody CardRequest request
    ) {
        logger.trace("getCards called: baseUrl={}, routingPrefix={}, request={}", baseUrl, routingPrefix, request);

        Stream<PullRequestId> pullRequestIds = request.getTokens("pull_request_urls")
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
                .map(pair -> makeCard(routingPrefix, pair, locale))
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
        return uriComponents.getHost().equalsIgnoreCase("github.com");
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

    private Flux<Pair<PullRequestId, PullRequest>> fetchPullRequest(
            String baseUrl,
            PullRequestId pullRequestId,
            String auth
    ) {
        logger.trace("fetchPullRequest called: baseUrl={}, id={}", baseUrl, pullRequestId);
        return rest.get()
                .uri(makeGithubUri(baseUrl, pullRequestId))
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToFlux(PullRequest.class)
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
            Locale locale
    ) {
        logger.trace("makeCard called: routingPrefix={}, info={}", routingPrefix, info);

        PullRequestId pullRequestId = info.getLeft();
        PullRequest pullRequest = info.getRight();
        boolean isOpen = OPEN_STATE.equalsIgnoreCase(pullRequest.getState());

        Card.Builder card = new Card.Builder()
                .setName("GithubPr") // TODO - remove this in APF-536
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(
                        cardTextAccessor.getHeader(locale),
                        cardTextAccessor.getMessage(
                                "subtitle", locale,
                                pullRequestId.getOwner(),
                                pullRequestId.getRepo(),
                                pullRequestId.getNumber()
                        )
                )
                .setBody(createBody(pullRequestId, pullRequest, locale));

        addCloseAction(card, routingPrefix, pullRequestId, isOpen, locale);
        addMergeAction(card, routingPrefix, pullRequestId, pullRequest, isOpen, locale);
        addApproveAction(card, routingPrefix, pullRequestId, isOpen, locale);
        addCommentAction(card, routingPrefix, pullRequestId, locale);
        addRequestChangesAction(card, routingPrefix, pullRequestId, isOpen, locale);

        return card.build();
    }

    private CardBody createBody(
            PullRequestId pullRequestId,
            PullRequest pullRequest,
            Locale locale
    ) {
        CardBody.Builder body = new CardBody.Builder();

        addInfo(body, pullRequestId, pullRequest, locale);
        addFinishedDates(body, pullRequest, locale);
        addChangeStats(body, pullRequest, locale);

        return body.build();
    }

    private void addInfo(
            CardBody.Builder body,
            PullRequestId pullRequestId,
            PullRequest pullRequest,
            Locale locale
    ) {
        body
                .setDescription(cardTextAccessor.getBody(locale, pullRequest.getBody()))
                .addField(buildGeneralBodyField("repository", locale, pullRequestId.getOwner(), pullRequestId.getRepo()))
                .addField(buildGeneralBodyField("requester", locale, pullRequest.getUser().getLogin()))
                .addField(buildGeneralBodyField("title", locale, pullRequest.getTitle()))
                .addField(buildGeneralBodyField("state", locale, pullRequest.getState()))
                .addField(buildGeneralBodyField("merged", locale, pullRequest.isMerged()))
                .addField(buildGeneralBodyField("mergeable", locale, pullRequest.getMergeable()))
                .addField(
                        buildGeneralBodyField(
                                "createdAt", locale,
                                DateTimeFormatter.ISO_INSTANT.format(pullRequest.getCreatedAt().toInstant())
                        )
                )
                .addField(buildGeneralBodyField("comments", locale, pullRequest.getComments()))
                .addField(buildGeneralBodyField("reviewComments", locale, pullRequest.getReviewComments()));
    }

    private CardBodyField buildGeneralBodyField(String messageKeyPrefix, Locale locale, Object... descriptionArgs) {
        return new CardBodyField.Builder()
                .setTitle(cardTextAccessor.getMessage(messageKeyPrefix + ".title", locale))
                .setType(CardBodyFieldType.GENERAL)
                .setDescription(
                        cardTextAccessor.getMessage(
                                messageKeyPrefix + ".description", locale,
                                descriptionArgs
                        )
                )
                .build();
    }

    private void addFinishedDates(
            CardBody.Builder body,
            PullRequest pullRequest,
            Locale locale
    ) {
        if (pullRequest.getClosedAt() != null) {
            if (pullRequest.isMerged()) {
                body.addField(
                        buildGeneralBodyField(
                                "mergedAt", locale,
                                DateTimeFormatter.ISO_INSTANT.format(pullRequest.getMergedAt().toInstant()),
                                pullRequest.getMergedBy().getLogin()
                        )
                );
            } else {
                body.addField(
                        buildGeneralBodyField(
                                "closedAt", locale,
                                DateTimeFormatter.ISO_INSTANT.format(pullRequest.getClosedAt().toInstant())
                        )
                );
            }
        }
    }

    private void addChangeStats(
            CardBody.Builder body,
            PullRequest pullRequest,
            Locale locale
    ) {
        body
                .addField(buildGeneralBodyField("commits", locale, pullRequest.getCommits()))
                .addField(buildGeneralBodyField("changes", locale, pullRequest.getAdditions(), pullRequest.getDeletions()))
                .addField(buildGeneralBodyField("filesChanged", locale, pullRequest.getChangedFiles()));
    }

    private void addCloseAction(
            Card.Builder card,
            String routingPrefix,
            PullRequestId pullRequestId,
            boolean isOpen,
            Locale locale
    ) {
        if (isOpen) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("close", locale))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("close", locale))
                            .setActionKey(CardActionKey.USER_INPUT)
                            .setUrl(getActionUrl(routingPrefix, pullRequestId, "close"))
                            .addUserInputField(
                                    new CardActionInputField.Builder()
                                            .setId(CLOSE_REASON_PARAM_KEY)
                                            .setLabel(cardTextAccessor.getActionLabel("close.reason", locale))
                                            .build()
                            )
                            .setType(HttpMethod.POST)
                            .build()
            );
        }
    }

    private void addMergeAction(
            Card.Builder card,
            String routingPrefix,
            PullRequestId pullRequestId,
            PullRequest pullRequest,
            boolean isOpen,
            Locale locale
    ) {
        if (isOpen && Boolean.TRUE.equals(pullRequest.getMergeable())) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("merge", locale))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("merge", locale))
                            .setActionKey(CardActionKey.DIRECT)
                            .setUrl(getActionUrl(routingPrefix, pullRequestId, "merge"))
                            .addRequestParam(SHA_PARAM_KEY, pullRequest.getHead().getSha())
                            .setType(HttpMethod.POST)
                            .build()
            );
        }
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
                                        .setLabel(cardTextAccessor.getActionLabel("comment.comment", locale))
                                        .setMinLength(1)
                                        .build()
                        )
                        .setType(HttpMethod.POST)
                        .build()
        );
    }

    private void addRequestChangesAction(
            Card.Builder card,
            String routingPrefix,
            PullRequestId pullRequestId,
            boolean isOpen,
            Locale locale
    ) {
        if (isOpen) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("requestChanges", locale))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("requestChanges", locale))
                            .setActionKey(CardActionKey.USER_INPUT)
                            .setUrl(getActionUrl(routingPrefix, pullRequestId, "request-changes"))
                            .addUserInputField(
                                    new CardActionInputField.Builder()
                                            .setId(REQUEST_PARAM_KEY)
                                            .setLabel(cardTextAccessor.getActionLabel("requestChanges.request", locale))
                                            .setMinLength(1)
                                            .build()
                            )
                            .setType(HttpMethod.POST)
                            .build()
            );
        }
    }

    private String getActionUrl(final String routingPrefix,
                                final PullRequestId pullRequestId,
                                final String action) {
        return routingPrefix + "api/v1/" + pullRequestId.getOwner() + "/" + pullRequestId.getRepo() + "/" + pullRequestId.getNumber() + "/" + action;
    }

    @PostMapping(
            path = "/api/v1/{owner}/{repo}/{number}/close",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<String> close(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            PullRequestId pullRequestId,
            @RequestParam(name = CLOSE_REASON_PARAM_KEY, required = false) String reason
    ) {
        logger.trace(
                "close called: baseUrl={}, id={}, reason={}",
                baseUrl,
                pullRequestId,
                reason
        );

        Mono<String> reasonResponse;

        if (StringUtils.isEmpty(reason)) {
            reasonResponse = Mono.just("does not matter");
        } else {
            reasonResponse = postReview(
                    pullRequestId,
                    Review.comment(reason),
                    auth,
                    baseUrl
            );
        }

        return reasonResponse
                .flatMap(ignored -> closePullRequest(auth, baseUrl, pullRequestId));
    }

    private Mono<String> closePullRequest(
            String auth,
            String baseUrl,
            PullRequestId pullRequestId
    ) {
       return rest.patch()
                .uri(makeGithubUri(baseUrl, pullRequestId))
                .header(AUTHORIZATION, auth)
                .contentType(APPLICATION_JSON)
                .syncBody(ImmutableMap.of("state", "closed"))
                .retrieve()
                .bodyToMono(String.class);
    }

    @PostMapping(
            path = "/api/v1/{owner}/{repo}/{number}/merge",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<String> merge(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            PullRequestId pullRequestId,
            @RequestParam(SHA_PARAM_KEY) String sha
    ) {
        logger.trace(
                "merge called: baseUrl={}, pull request id={}",
                baseUrl,
                pullRequestId
        );

        return rest.put()
                .uri(makeGithubUri(baseUrl, pullRequestId) + "/merge")
                .header(AUTHORIZATION, auth)
                .contentType(APPLICATION_JSON)
                .syncBody(ImmutableMap.of("sha", sha))
                .retrieve()
                .bodyToMono(String.class);
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

    @PostMapping(
            path = "/api/v1/{owner}/{repo}/{number}/request-changes",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<String> requestChanges(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            PullRequestId pullRequestId,
            @RequestParam(REQUEST_PARAM_KEY) String request
    ) {
        logger.trace(
                "requestChanges called: baseUrl={}, id={}, request={}",
                baseUrl,
                pullRequestId,
                request
        );

        return postReview(
                pullRequestId,
                Review.requestChanges(request),
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
