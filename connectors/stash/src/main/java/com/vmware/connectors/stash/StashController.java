/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.stash;

import com.google.common.collect.ImmutableMap;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.Async;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.web.ObservableUtil;
import com.vmware.connectors.stash.utils.StashAction;
import com.vmware.connectors.stash.utils.StashComment;
import com.vmware.connectors.stash.utils.StashConstants;
import com.vmware.connectors.stash.utils.StashPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.AsyncRestOperations;
import rx.Observable;
import rx.Single;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vmware.connectors.stash.utils.StashConstants.ATLASSIAN_TOKEN;
import static com.vmware.connectors.stash.utils.StashConstants.COMMENT_PARAM_KEY;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
public class StashController {

    private static final Logger logger = LoggerFactory.getLogger(StashController.class);

    // Authorization header for stash.
    private static final String AUTH_HEADER = "x-stash-authorization";

    // Stash base URL.
    private static final String BASE_URL_HEADER = "x-stash-base-url";

    // Routing prefix for stash connector.
    private static final String ROUTING_PREFIX = "x-routing-prefix";

    private static final String OPEN = "OPEN";

    private static final String STASH_PREFIX = "stash.";

    private static final int COMMENTS_SIZE = 2;

    private static final String STASH_COMMENTS = "stash.comments";

    @Resource
    private AsyncRestOperations rest;

    @Resource
    private CardTextAccessor cardTextAccessor;

    @PostMapping(
            value = "/cards/requests",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Single<ResponseEntity<Cards>> getCards(
            @RequestHeader(AUTH_HEADER) final String authHeader,
            @RequestHeader(BASE_URL_HEADER) final String baseUrl,
            @RequestHeader(ROUTING_PREFIX) final String routingPrefix,
            @Valid @RequestBody final CardRequest cardRequest) {

        logger.trace("Cards requests for stash connector - authHeader: {}, baseUrlHeader: {}, routingPrefix: {}",
                authHeader,
                baseUrl,
                routingPrefix);

        final Set<String> cardTokens = cardRequest.getTokens(StashConstants.STASH_PR_EMAIL_SUBJECT);
        if (CollectionUtils.isEmpty(cardTokens)) {
            return Single.just(ResponseEntity.ok(new Cards()));
        }

        final Set<StashPullRequest> pullRequests = convertToStashPullRequest(cardTokens);

        final HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, authHeader);

        return Observable.from(pullRequests)
                .flatMap(pullRequest -> getCardsForStashPR(headers, pullRequest, baseUrl, routingPrefix))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .map(ResponseEntity::ok)
                .toSingle();
    }

    @PostMapping(
        path = "/api/v1/{projectKey}/{repositorySlug}/{pullRequestId}/approve",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> approve(final StashPullRequest pullRequest,
                                                  @RequestHeader(AUTH_HEADER) final String authHeader,
                                                  @RequestHeader(BASE_URL_HEADER) final String baseUrl) {

        logger.debug("Approve action for stash pull request: {}, baseURL: {}",
                pullRequest,
                baseUrl);

        return performStashAction(baseUrl, authHeader, pullRequest, StashAction.APPROVE);
    }

    @PostMapping(
            path = "/api/v1/{projectKey}/{repositorySlug}/{pullRequestId}/merge",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> merge(final StashPullRequest pullRequest,
                                                  @RequestHeader(AUTH_HEADER) final String authHeader,
                                                  @RequestHeader(BASE_URL_HEADER) final String baseUrl) {

        logger.debug("Merge action for stash pull request: {}, baseURL: {}",
                pullRequest,
                baseUrl);

        return performStashAction(baseUrl, authHeader, pullRequest, StashAction.MERGE);
    }

    @PostMapping(
            path = "/api/v1/{projectKey}/{repositorySlug}/{pullRequestId}/decline",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> decline(final StashPullRequest pullRequest,
                                                @RequestHeader(AUTH_HEADER) final String authHeader,
                                                @RequestHeader(BASE_URL_HEADER) final String baseUrl) {

        logger.debug("Decline action for stash pull request: {}, baseURL: {}",
                pullRequest,
                baseUrl);

        return performStashAction(baseUrl, authHeader, pullRequest, StashAction.DECLINE);
    }

    @PostMapping(
            path = "/api/v1/{projectKey}/{repositorySlug}/{pullRequestId}/comments",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> comments(final StashPullRequest pullRequest,
                                                  @RequestParam(COMMENT_PARAM_KEY) final String comment,
                                                  @RequestHeader(AUTH_HEADER) final String authHeader,
                                                  @RequestHeader(BASE_URL_HEADER) final String baseUrl) {

        logger.debug("Comment action for stash pull request: {}, baseURL: {}",
                pullRequest,
                baseUrl);

        final HttpHeaders headers = buildHeaders(authHeader);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        final String url = String.format(StashConstants.STASH_PR_COMMENT_URL_FORMAT,
                baseUrl,
                pullRequest.getProjectKey(),
                pullRequest.getRepositorySlug(),
                pullRequest.getPullRequestId());

        final StashComment stashComment = new StashComment(comment);

        final ListenableFuture<ResponseEntity<String>> response = this.rest.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(stashComment, headers),
                String.class);
        return Async.toSingle(response);
    }

    private List<String> getComments(final String baseUrl,
                                     final HttpHeaders headers,
                                     final StashPullRequest pullRequest) {
        final String url = String.format(StashConstants.STASH_ACTIVITIES_URL_FORMAT,
                baseUrl,
                pullRequest.getProjectKey(),
                pullRequest.getRepositorySlug(),
                pullRequest.getPullRequestId());

        final ListenableFuture<ResponseEntity<JsonDocument>> response = this.rest.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonDocument.class);

        final JsonDocument[] jsonDocuments = new JsonDocument[1];
        Async.toSingle(response)
                .subscribe(entity -> {
                    jsonDocuments[0] = entity.getBody();
                });
        Assert.isTrue(Objects.nonNull(jsonDocuments[0]), "JsonDocument should not be null.");

        final List<String> comments = jsonDocuments[0].read("$.values[*].comment.text");
        return comments.stream()
                .limit(COMMENTS_SIZE)
                .collect(Collectors.toList());
    }

    private Single<ResponseEntity<String>> performStashAction(final String baseUrl,
                                                              final String authHeader,
                                                              final StashPullRequest pullRequest,
                                                              final StashAction stashAction) {
        final HttpHeaders headers = buildHeaders(authHeader);

        // Get current version of the pull request. Pull request "version" changes when we do any actions on it.
        // When the pull request is raised, the current value will be 0.
        // For example, when we approve the pull request, then the version will change from 0 to 1.
        // We have to add the latest version of the pull request URI to do any actions. Otherwise, the action will be rejected.
        // If the build for the branch is going on, then the actions would be rejected.
        final String version = getVersion(headers, baseUrl, pullRequest);

        final String url = String.format(StashConstants.STASH_PR_ACTION_FORMAT,
                baseUrl,
                pullRequest.getProjectKey(),
                pullRequest.getRepositorySlug(),
                pullRequest.getPullRequestId(),
                stashAction.getAction(),
                version);

        // This header is added for skipping the CSRF check. Otherwise, Atlassian denies the pull request action.
        headers.add(ATLASSIAN_TOKEN, "no-check");

        final ListenableFuture<ResponseEntity<String>> response = this.rest.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);
        return Async.toSingle(response);
    }

    private HttpHeaders buildHeaders(final String authHeader) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, authHeader);
        return headers;
    }

    private String getVersion(final HttpHeaders headers,
                              final String baseUrl,
                              final StashPullRequest pullRequest) {

        final ListenableFuture<ResponseEntity<JsonDocument>> response = getPullRequestInfo(headers, pullRequest, baseUrl);
        final JsonDocument[] jsonDocuments = new JsonDocument[1];
        Async.toSingle(response)
                .subscribe(entity -> {
                    jsonDocuments[0] = entity.getBody();
                });

        Assert.isTrue(Objects.nonNull(jsonDocuments[0]), "JsonDocument should not be null.");
        return Integer.toString(jsonDocuments[0].read("$.version"));
    }

    private Observable<Card> getCardsForStashPR(final HttpHeaders headers,
                                                final StashPullRequest pullRequest,
                                                final String baseUrl,
                                                final String routingPrefix) {
        logger.debug("Requesting pull request info from stash base url: {} and pull request info: {}", baseUrl, pullRequest);

        final ListenableFuture<ResponseEntity<JsonDocument>> stashResponse = getPullRequestInfo(headers, pullRequest, baseUrl);
        final List<String> comments = getComments(baseUrl, headers, pullRequest);

        return Async.toSingle(stashResponse)
                .toObservable()
                .onErrorResumeNext(ObservableUtil::skip404)
                .map(entity -> convertResponseIntoCard(entity, pullRequest, routingPrefix, comments));
    }

    private ListenableFuture<ResponseEntity<JsonDocument>> getPullRequestInfo(final HttpHeaders headers,
                                                                              final StashPullRequest pullRequest,
                                                                              final String baseUrl) {
        final String url = String.format(StashConstants.STASH_PR_URL_FORMAT,
                baseUrl,
                pullRequest.getProjectKey(),
                pullRequest.getRepositorySlug(),
                pullRequest.getPullRequestId());

        return this.rest.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonDocument.class);
    }

    private Card convertResponseIntoCard(final ResponseEntity<JsonDocument> entity,
                                         final StashPullRequest pullRequest,
                                         final String routingPrefix,
                                         final List<String> comments) {
        final JsonDocument stashResponse = entity.getBody();

        final String author = stashResponse.read("$.author.user.displayName");
        final String title = stashResponse.read("$.title");
        final String description = stashResponse.read("$.description");
        final boolean isPROpen = OPEN.equalsIgnoreCase(stashResponse.read("$.state"));

        final Card.Builder card = new Card.Builder()
                .setHeader(
                        this.cardTextAccessor.getHeader(
                                title,
                                author),
                        this.cardTextAccessor.getMessage(
                                "stash.card.subtitle",
                                description))
                .setBody(makeCardBody(stashResponse, comments));

        addCommentAction(card, routingPrefix, pullRequest);

        // Add the following actions, only if the pull request state is open.
        if (isPROpen) {
            // Add approve action.
            addPullRequestAction(card,
                    routingPrefix,
                    pullRequest,
                    StashAction.APPROVE);

            // Add decline action.
            addPullRequestAction(card,
                    routingPrefix,
                    pullRequest,
                    StashAction.DECLINE);

            // Add merge action.
            addPullRequestAction(card,
                    routingPrefix,
                    pullRequest,
                    StashAction.MERGE);
        }

        return card.build();
    }

    private void addPullRequestAction(final Card.Builder card,
                                      final String routingPrefix,
                                      final StashPullRequest pullRequest,
                                      final StashAction stashAction) {
        card.addAction(
                new CardAction.Builder()
                    .setLabel(this.cardTextAccessor.getActionLabel(STASH_PREFIX + stashAction.getAction()))
                    .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel(STASH_PREFIX + stashAction.getAction()))
                    .setActionKey(CardActionKey.DIRECT)
                    .setActionKey(buildActionUrl(routingPrefix, pullRequest, stashAction))
                    .setType(HttpMethod.POST)
                    .build()
        );
    }

    private void addCommentAction(final Card.Builder card,
                                  final String routingPrefix,
                                  final StashPullRequest pullRequest) {
        card.addAction(
                new CardAction.Builder()
                    .setLabel(this.cardTextAccessor.getActionLabel(STASH_COMMENTS))
                    .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel(STASH_COMMENTS))
                    .setActionKey(CardActionKey.USER_INPUT)
                    .setUrl(buildActionUrl(routingPrefix, pullRequest, StashAction.COMMENTS))
                    .addUserInputField(
                            new CardActionInputField.Builder()
                                .setId("Comment")
                                .setLabel(this.cardTextAccessor.getMessage(STASH_COMMENTS))
                                .setMinLength(1)
                                .build()
                    )
                    .setType(HttpMethod.POST)
                    .build()
                );
    }

    private String buildActionUrl(final String routingPrefix,
                                  final StashPullRequest pullRequest,
                                  final StashAction stashAction) {
        return String.format(StashConstants.STASH_CLIENT_ACTION_URL,
                routingPrefix,
                pullRequest.getProjectKey(),
                pullRequest.getRepositorySlug(),
                pullRequest.getPullRequestId(),
                stashAction.getAction());
    }

    private CardBody makeCardBody(final JsonDocument stashResponse, final List<String> comments) {
        final CardBody.Builder cardBody = new CardBody.Builder();
        cardBody.setDescription(stashResponse.read("$.description"));

        cardBody.addField(makeCardBodyField("stash.repository", stashResponse.read("$.fromRef.repository.slug")));
        cardBody.addField(makeCardBodyField("stash.author", stashResponse.read("$.author.user.displayName")));
        cardBody.addField(makeCardBodyField("stash.title", stashResponse.read("$.title")));
        cardBody.addField(makeCardBodyField("stash.state", stashResponse.read("$.state")));
        cardBody.addField(makeCardBodyField("stash.open", Boolean.toString(stashResponse.read("$.open"))));

        if (!CollectionUtils.isEmpty(comments)) {
            cardBody.addField(addCommentField(comments));
        }
        return cardBody.build();
    }

    private CardBodyField addCommentField(final List<String> comments) {
        final CardBodyField.Builder cardBodyFieldBuilder = new CardBodyField.Builder();
        cardBodyFieldBuilder.setTitle(this.cardTextAccessor.getMessage(STASH_COMMENTS));
        cardBodyFieldBuilder.setType(CardBodyFieldType.COMMENT);

        comments.forEach(comment -> {
            cardBodyFieldBuilder.addContent(ImmutableMap.of("text", comment));
        });
        return cardBodyFieldBuilder.build();
    }

    private CardBodyField makeCardBodyField(final String messageKeyPrefix, final String descriptionArgs) {
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

    private Set<StashPullRequest> convertToStashPullRequest(final Set<String> cardTokens) {
        final Set<StashPullRequest> pullRequests = new HashSet<>();
        final Pattern pattern = Pattern.compile(StashConstants.STASH_PR_EMAIL_SUBJECT_REGEX);

        for (final String prEmailSubject: cardTokens) {
            final Matcher matcher = pattern.matcher(prEmailSubject);
            while(matcher.find()) {
                final StashPullRequest pullRequest = new StashPullRequest(
                        matcher.group(2),
                        matcher.group(3),
                        matcher.group(4));
                pullRequests.add(pullRequest);
            }
        }
        return pullRequests;
    }
}
