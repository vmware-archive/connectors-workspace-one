/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.github.pr;

import com.google.common.collect.ImmutableMap;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardAction;
import com.vmware.connectors.common.payloads.response.CardActionInputField;
import com.vmware.connectors.common.payloads.response.CardActionKey;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.CardBodyField;
import com.vmware.connectors.common.payloads.response.CardBodyFieldType;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.utils.Async;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.web.ObservableUtil;
import com.vmware.connectors.github.pr.v3.PullRequest;
import com.vmware.connectors.github.pr.v3.Review;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import rx.Observable;
import rx.Single;

import javax.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
public class GithubPrController {

    private static final Logger logger = LoggerFactory.getLogger(GithubPrController.class);

    private static final String AUTH_HEADER = "x-github-pr-authorization";
    private static final String BASE_URL_HEADER = "x-github-pr-base-url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";

    private static final String CLOSE_REASON_PARAM_KEY = "reason";
    private static final String COMMENT_PARAM_KEY = "message";
    private static final String REQUEST_PARAM_KEY = "request";

    private final AsyncRestOperations rest;
    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public GithubPrController(
            AsyncRestOperations rest,
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
    public Single<ResponseEntity<Cards>> getCards(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            @Valid @RequestBody CardRequest request
    ) {
        logger.trace("getCards called: baseUrl={}, routingPrefix={}, request={}", baseUrl, routingPrefix, request);

        List<PullRequestId> pullRequestUrls = request.getTokens("pull_request_urls")
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()) // squash duplicates
                .stream()
                .sorted()
                .map(this::parseUri)
                .filter(Objects::nonNull)
                .filter(this::validHost)
                .map(this::getPullRequestId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(pullRequestUrls)) {
            return Single.just(ResponseEntity.ok(new Cards()));
        }

        HttpHeaders headers = makeHeaders(auth);
        HttpEntity<HttpHeaders> httpHeaders = new HttpEntity<>(headers);

        return fetchAllPullRequests(baseUrl, httpHeaders, pullRequestUrls)
                .map(pair -> makeCard(routingPrefix, pair))
                .reduce(
                        new Cards(),
                        (cards, card) -> {
                            cards.getCards().add(card);
                            return cards;
                        }
                )
                .map(ResponseEntity::ok)
                .toSingle();
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
        if (uri.getPathSegments().size() != 4) {
            return null;
        }
        PullRequestId id = new PullRequestId();
        id.setOwner(uri.getPathSegments().get(0));
        id.setRepo(uri.getPathSegments().get(1));
        id.setNumber(uri.getPathSegments().get(3));
        return id;
    }

    private HttpHeaders makeHeaders(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, auth);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    private Observable<Pair<PullRequestId, PullRequest>> fetchAllPullRequests(
            String baseUrl,
            HttpEntity<HttpHeaders> headers,
            List<PullRequestId> ids
    ) {
        logger.trace("fetchAllPullRequests called: baseUrl={}, ids={}", baseUrl, ids);

        return Observable.from(ids)
                .flatMap(id -> fetchPullRequest(baseUrl, id, headers));
    }

    private Observable<Pair<PullRequestId, PullRequest>> fetchPullRequest(
            String baseUrl,
            PullRequestId id,
            HttpEntity<HttpHeaders> headers
    ) {
        logger.trace("fetchPullRequest called: baseUrl={}, id={}", baseUrl, id);

        ListenableFuture<ResponseEntity<PullRequest>> response = rest.exchange(
                makeGithubUri(baseUrl, id),
                HttpMethod.GET,
                headers,
                PullRequest.class
        );

        return Async.toSingle(response)
                .toObservable()
                .onErrorResumeNext(ObservableUtil::skip404)
                .map(ResponseEntity::getBody)
                .map(pullRequest -> Pair.of(id, pullRequest));
    }

    private String makeGithubUri(
            String baseUrl,
            PullRequestId id
    ) {
        return UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/repos/{owner}/{repo}/pulls/{number}")
                .buildAndExpand(
                        ImmutableMap.of(
                                "owner", id.getOwner(),
                                "repo", id.getRepo(),
                                "number", id.getNumber()
                        )
                )
                .encode()
                .toUri()
                .toString();
    }

    private Card makeCard(
            String routingPrefix,
            Pair<PullRequestId, PullRequest> info
    ) {
        logger.trace("makeCard called: routingPrefix={}, info={}", routingPrefix, info);

        PullRequestId id = info.getLeft();
        PullRequest pullRequest = info.getRight();
        boolean isOpen = "open".equalsIgnoreCase(pullRequest.getState());

        Card.Builder card = new Card.Builder()
                .setName("GithubPr") // TODO - remove this in APF-536
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(
                        cardTextAccessor.getHeader(
                                id.getOwner(),
                                id.getRepo(),
                                id.getNumber()
                        ),
                        cardTextAccessor.getMessage(
                                "subtitle",
                                id.getOwner(),
                                id.getRepo(),
                                id.getNumber()
                        )
                )
                .setBody(createBody(id, pullRequest));

        addCloseAction(card, routingPrefix, id, isOpen);
        addMergeAction(card, routingPrefix, id, pullRequest, isOpen);
        addApproveAction(card, routingPrefix, id, isOpen);
        addCommentAction(card, routingPrefix, id);
        addRequestChangesAction(card, routingPrefix, id, isOpen);

        return card.build();
    }

    private CardBody createBody(
            PullRequestId id,
            PullRequest pullRequest
    ) {
        CardBody.Builder body = new CardBody.Builder();

        addInfo(body, id, pullRequest);
        addFinishedDates(body, pullRequest);
        addChangeStats(body, pullRequest);

        return body.build();
    }

    private void addInfo(
            CardBody.Builder body,
            PullRequestId id,
            PullRequest pullRequest
    ) {
        body
                .setDescription(cardTextAccessor.getBody(pullRequest.getBody()))
                .addField(buildGeneralBodyField("repository", id.getOwner(), id.getRepo()))
                .addField(buildGeneralBodyField("requester", pullRequest.getUser().getLogin()))
                .addField(buildGeneralBodyField("title", pullRequest.getTitle()))
                .addField(buildGeneralBodyField("state", pullRequest.getState()))
                .addField(buildGeneralBodyField("merged", pullRequest.isMerged()))
                .addField(buildGeneralBodyField("mergeable", pullRequest.getMergeable()))
                .addField(
                        buildGeneralBodyField(
                                "createdAt",
                                DateTimeFormatter.ISO_INSTANT.format(pullRequest.getCreatedAt().toInstant())
                        )
                )
                .addField(buildGeneralBodyField("comments", pullRequest.getComments()))
                .addField(buildGeneralBodyField("reviewComments", pullRequest.getReviewComments()));
    }

    private CardBodyField buildGeneralBodyField(String messageKeyPrefix, Object... descriptionArgs) {
        return new CardBodyField.Builder()
                .setTitle(cardTextAccessor.getMessage(messageKeyPrefix + ".title"))
                .setType(CardBodyFieldType.GENERAL)
                .setDescription(
                        cardTextAccessor.getMessage(
                                messageKeyPrefix + ".description",
                                descriptionArgs
                        )
                )
                .build();
    }

    private void addFinishedDates(
            CardBody.Builder body,
            PullRequest pullRequest
    ) {
        if (pullRequest.getClosedAt() != null) {
            if (pullRequest.isMerged()) {
                body.addField(
                        buildGeneralBodyField(
                                "mergedAt",
                                DateTimeFormatter.ISO_INSTANT.format(pullRequest.getMergedAt().toInstant()),
                                pullRequest.getMergedBy().getLogin()
                        )
                );
            } else {
                body.addField(
                        buildGeneralBodyField(
                                "closedAt",
                                DateTimeFormatter.ISO_INSTANT.format(pullRequest.getClosedAt().toInstant())
                        )
                );
            }
        }
    }

    private void addChangeStats(
            CardBody.Builder body,
            PullRequest pullRequest
    ) {
        body
                .addField(buildGeneralBodyField("commits", pullRequest.getCommits()))
                .addField(buildGeneralBodyField("changes", pullRequest.getAdditions(), pullRequest.getDeletions()))
                .addField(buildGeneralBodyField("filesChanged", pullRequest.getChangedFiles()));
    }

    private void addCloseAction(
            Card.Builder card,
            String routingPrefix,
            PullRequestId id,
            boolean isOpen
    ) {
        if (isOpen) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("close"))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("close"))
                            .setActionKey(CardActionKey.USER_INPUT)
                            .setUrl(getActionUrl(routingPrefix, id, "close"))
                            .addUserInputField(
                                    new CardActionInputField.Builder()
                                            .setId(CLOSE_REASON_PARAM_KEY)
                                            .setLabel(cardTextAccessor.getActionLabel("close.reason"))
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
            PullRequestId id,
            PullRequest pullRequest,
            boolean isOpen
    ) {
        if (isOpen && Boolean.TRUE.equals(pullRequest.getMergeable())) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("merge"))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("merge"))
                            .setActionKey(CardActionKey.DIRECT)
                            .setUrl(getActionUrl(routingPrefix, id, "merge"))
                            .addRequestParam("sha", pullRequest.getHead().getSha())
                            .setType(HttpMethod.POST)
                            .build()
            );
        }
    }

    private void addApproveAction(
            Card.Builder card,
            String routingPrefix,
            PullRequestId id,
            boolean isOpen
    ) {
        if (isOpen) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("approve"))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("approve"))
                            .setActionKey(CardActionKey.DIRECT)
                            .setUrl(getActionUrl(routingPrefix, id, "approve"))
                            .setType(HttpMethod.POST)
                            .build()
            );
        }
    }

    private void addCommentAction(
            Card.Builder card,
            String routingPrefix,
            PullRequestId id
    ) {
        card.addAction(
                new CardAction.Builder()
                        .setLabel(cardTextAccessor.getActionLabel("comment"))
                        .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("comment"))
                        .setActionKey(CardActionKey.USER_INPUT)
                        .setUrl(getActionUrl(routingPrefix, id, "comment"))
                        .addUserInputField(
                                new CardActionInputField.Builder()
                                        .setId(COMMENT_PARAM_KEY)
                                        .setLabel(cardTextAccessor.getActionLabel("comment.comment"))
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
            PullRequestId id,
            boolean isOpen
    ) {
        if (isOpen) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("requestChanges"))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("requestChanges"))
                            .setActionKey(CardActionKey.USER_INPUT)
                            .setUrl(getActionUrl(routingPrefix, id, "request-changes"))
                            .addUserInputField(
                                    new CardActionInputField.Builder()
                                            .setId(REQUEST_PARAM_KEY)
                                            .setLabel(cardTextAccessor.getActionLabel("requestChanges.request"))
                                            .setMinLength(1)
                                            .build()
                            )
                            .setType(HttpMethod.POST)
                            .build()
            );
        }
    }

    private String getActionUrl(String routingPrefix, PullRequestId id, String action) {
        return routingPrefix + "api/v1/" + id.getOwner() + "/" + id.getRepo() + "/" + id.getNumber() + "/" + action;
    }

    @PostMapping(
            path = "/api/v1/{owner}/{repo}/{number}/close",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> close(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            PullRequestId id,
            @RequestParam(name = CLOSE_REASON_PARAM_KEY, required = false) String reason
    ) {
        logger.trace(
                "close called: baseUrl={}, id={}, reason={}",
                baseUrl,
                id,
                reason
        );

        Single<ResponseEntity<String>> reasonResponse;

        if (StringUtils.isEmpty(reason)) {
            reasonResponse = Single.just(ResponseEntity.ok("does not matter"));
        } else {
            reasonResponse = postReview(
                    id,
                    Review.comment(reason),
                    auth,
                    baseUrl
            );
        }

        return reasonResponse
                .flatMap(ignored -> closePullRequest(auth, baseUrl, id));
    }

    private Single<ResponseEntity<String>> closePullRequest(
            String auth,
            String baseUrl,
            PullRequestId id
    ) {
        HttpHeaders headers = makeHeaders(auth);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(ImmutableMap.of("state", "closed"), headers);

        ListenableFuture<ResponseEntity<String>> response = rest.exchange(
                makeGithubUri(baseUrl, id),
                HttpMethod.PATCH,
                request,
                String.class
        );

        return Async.toSingle(response);
    }

    @PostMapping(
            path = "/api/v1/{owner}/{repo}/{number}/merge",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> merge(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            PullRequestId id,
            @RequestParam("sha") String sha
    ) {
        logger.trace(
                "merge called: baseUrl={}, owner={}, repo={}, number={}",
                baseUrl,
                id
        );

        HttpHeaders headers = makeHeaders(auth);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(ImmutableMap.of("sha", sha), headers);

        ListenableFuture<ResponseEntity<String>> response = rest.exchange(
                makeGithubUri(baseUrl, id) + "/merge",
                HttpMethod.PUT,
                request,
                String.class
        );

        return Async.toSingle(response);
    }

    @PostMapping(
            path = "/api/v1/{owner}/{repo}/{number}/approve",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> approve(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            PullRequestId id
    ) {
        logger.trace(
                "approve called: baseUrl={}, id={}",
                baseUrl,
                id
        );

        return postReview(
                id,
                Review.approve(),
                auth,
                baseUrl
        );
    }

    @PostMapping(
            path = "/api/v1/{owner}/{repo}/{number}/comment",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> comment(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            PullRequestId id,
            @RequestParam(COMMENT_PARAM_KEY) String comment
    ) {
        logger.trace(
                "comment called: baseUrl={}, id={}, comment={}",
                baseUrl,
                id,
                comment
        );

        return postReview(
                id,
                Review.comment(comment),
                auth,
                baseUrl
        );
    }

    @PostMapping(
            path = "/api/v1/{owner}/{repo}/{number}/request-changes",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> requestChanges(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            PullRequestId id,
            @RequestParam(REQUEST_PARAM_KEY) String request
    ) {
        logger.trace(
                "requestChanges called: baseUrl={}, id={}, request={}",
                baseUrl,
                id,
                request
        );

        return postReview(
                id,
                Review.requestChanges(request),
                auth,
                baseUrl
        );
    }

    private Single<ResponseEntity<String>> postReview(
            PullRequestId id,
            Review review,
            String auth,
            String baseUrl
    ) {
        HttpHeaders headers = makeHeaders(auth);
        HttpEntity<Review> request = new HttpEntity<>(review, headers);

        ListenableFuture<ResponseEntity<String>> response = rest.exchange(
                makeGithubUri(baseUrl, id) + "/reviews",
                HttpMethod.POST,
                request,
                String.class
        );

        return Async.toSingle(response);
    }

}
