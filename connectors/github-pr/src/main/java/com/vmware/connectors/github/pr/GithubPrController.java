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
import com.vmware.connectors.common.utils.ObservableUtil;
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

    private static final String OPEN_STATE = "open";

    private static final String CLOSE_REASON_PARAM_KEY = "reason";
    private static final String COMMENT_PARAM_KEY = "message";
    private static final String REQUEST_PARAM_KEY = "request";
    private static final String SHA_PARAM_KEY = "sha";

    private static final int URI_SEGMENT_SIZE = 4;

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

        List<PullRequestId> pullRequestIds = request.getTokens("pull_request_urls")
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

        if (CollectionUtils.isEmpty(pullRequestIds)) {
            return Single.just(ResponseEntity.ok(new Cards()));
        }

        HttpHeaders headers = makeHeaders(auth);
        HttpEntity<HttpHeaders> httpHeaders = new HttpEntity<>(headers);

        return fetchAllPullRequests(baseUrl, httpHeaders, pullRequestIds)
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
        if (uri.getPathSegments().size() != URI_SEGMENT_SIZE) {
            return null;
        }
        PullRequestId pullRequestId = new PullRequestId();
        pullRequestId.setOwner(uri.getPathSegments().get(0));
        pullRequestId.setRepo(uri.getPathSegments().get(1));
        pullRequestId.setNumber(uri.getPathSegments().get(3));
        return pullRequestId;
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
                .flatMap(pullRequestId -> fetchPullRequest(baseUrl, pullRequestId, headers));
    }

    private Observable<Pair<PullRequestId, PullRequest>> fetchPullRequest(
            String baseUrl,
            PullRequestId pullRequestId,
            HttpEntity<HttpHeaders> headers
    ) {
        logger.trace("fetchPullRequest called: baseUrl={}, id={}", baseUrl, pullRequestId);

        ListenableFuture<ResponseEntity<PullRequest>> response = rest.exchange(
                makeGithubUri(baseUrl, pullRequestId),
                HttpMethod.GET,
                headers,
                PullRequest.class
        );

        return Async.toSingle(response)
                .toObservable()
                .onErrorResumeNext(ObservableUtil::skip404)
                .map(ResponseEntity::getBody)
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
            Pair<PullRequestId, PullRequest> info
    ) {
        logger.trace("makeCard called: routingPrefix={}, info={}", routingPrefix, info);

        PullRequestId pullRequestId = info.getLeft();
        PullRequest pullRequest = info.getRight();
        boolean isOpen = OPEN_STATE.equalsIgnoreCase(pullRequest.getState());

        Card.Builder card = new Card.Builder()
                .setName("GithubPr") // TODO - remove this in APF-536
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(
                        cardTextAccessor.getHeader(),
                        cardTextAccessor.getMessage(
                                "subtitle",
                                pullRequestId.getOwner(),
                                pullRequestId.getRepo(),
                                pullRequestId.getNumber()
                        )
                )
                .setBody(createBody(pullRequestId, pullRequest));

        addCloseAction(card, routingPrefix, pullRequestId, isOpen);
        addMergeAction(card, routingPrefix, pullRequestId, pullRequest, isOpen);
        addApproveAction(card, routingPrefix, pullRequestId, isOpen);
        addCommentAction(card, routingPrefix, pullRequestId);
        addRequestChangesAction(card, routingPrefix, pullRequestId, isOpen);

        return card.build();
    }

    private CardBody createBody(
            PullRequestId pullRequestId,
            PullRequest pullRequest
    ) {
        CardBody.Builder body = new CardBody.Builder();

        addInfo(body, pullRequestId, pullRequest);
        addFinishedDates(body, pullRequest);
        addChangeStats(body, pullRequest);

        return body.build();
    }

    private void addInfo(
            CardBody.Builder body,
            PullRequestId pullRequestId,
            PullRequest pullRequest
    ) {
        body
                .setDescription(cardTextAccessor.getBody(pullRequest.getBody()))
                .addField(buildGeneralBodyField("repository", pullRequestId.getOwner(), pullRequestId.getRepo()))
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
            PullRequestId pullRequestId,
            boolean isOpen
    ) {
        if (isOpen) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("close"))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("close"))
                            .setActionKey(CardActionKey.USER_INPUT)
                            .setUrl(getActionUrl(routingPrefix, pullRequestId, "close"))
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
            PullRequestId pullRequestId,
            PullRequest pullRequest,
            boolean isOpen
    ) {
        if (isOpen && Boolean.TRUE.equals(pullRequest.getMergeable())) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("merge"))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("merge"))
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
            boolean isOpen
    ) {
        if (isOpen) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("approve"))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("approve"))
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
            PullRequestId pullRequestId
    ) {
        card.addAction(
                new CardAction.Builder()
                        .setLabel(cardTextAccessor.getActionLabel("comment"))
                        .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("comment"))
                        .setActionKey(CardActionKey.USER_INPUT)
                        .setUrl(getActionUrl(routingPrefix, pullRequestId, "comment"))
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
            PullRequestId pullRequestId,
            boolean isOpen
    ) {
        if (isOpen) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("requestChanges"))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("requestChanges"))
                            .setActionKey(CardActionKey.USER_INPUT)
                            .setUrl(getActionUrl(routingPrefix, pullRequestId, "request-changes"))
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

    private String getActionUrl(final String routingPrefix,
                                final PullRequestId pullRequestId,
                                final String action) {
        return routingPrefix + "api/v1/" + pullRequestId.getOwner() + "/" + pullRequestId.getRepo() + "/" + pullRequestId.getNumber() + "/" + action;
    }

    @PostMapping(
            path = "/api/v1/{owner}/{repo}/{number}/close",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> close(
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

        Single<ResponseEntity<String>> reasonResponse;

        if (StringUtils.isEmpty(reason)) {
            reasonResponse = Single.just(ResponseEntity.ok("does not matter"));
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

    private Single<ResponseEntity<String>> closePullRequest(
            String auth,
            String baseUrl,
            PullRequestId pullRequestId
    ) {
        HttpHeaders headers = makeHeaders(auth);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(ImmutableMap.of("state", "closed"), headers);

        ListenableFuture<ResponseEntity<String>> response = rest.exchange(
                makeGithubUri(baseUrl, pullRequestId),
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
            PullRequestId pullRequestId,
            @RequestParam(SHA_PARAM_KEY) String sha
    ) {
        logger.trace(
                "merge called: baseUrl={}, pull request id={}",
                baseUrl,
                pullRequestId
        );

        HttpHeaders headers = makeHeaders(auth);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(ImmutableMap.of("sha", sha), headers);

        ListenableFuture<ResponseEntity<String>> response = rest.exchange(
                makeGithubUri(baseUrl, pullRequestId) + "/merge",
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
    public Single<ResponseEntity<String>> comment(
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
    public Single<ResponseEntity<String>> requestChanges(
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

    private Single<ResponseEntity<String>> postReview(
            PullRequestId pullRequestId,
            Review review,
            String auth,
            String baseUrl
    ) {
        HttpHeaders headers = makeHeaders(auth);
        HttpEntity<Review> request = new HttpEntity<>(review, headers);

        ListenableFuture<ResponseEntity<String>> response = rest.exchange(
                makeGithubUri(baseUrl, pullRequestId) + "/reviews",
                HttpMethod.POST,
                request,
                String.class
        );

        return Async.toSingle(response);
    }

}
