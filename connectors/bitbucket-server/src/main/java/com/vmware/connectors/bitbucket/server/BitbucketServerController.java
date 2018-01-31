/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.bitbucket.server;

import com.google.common.collect.ImmutableMap;
import com.vmware.connectors.bitbucket.server.utils.BitbucketServerAction;
import com.vmware.connectors.bitbucket.server.utils.BitbucketServerComment;
import com.vmware.connectors.bitbucket.server.utils.BitbucketServerPullRequest;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.Async;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.web.ObservableUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;
import rx.Observable;
import rx.Single;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vmware.connectors.bitbucket.server.utils.BitbucketServerConstants.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
public class BitbucketServerController {

    private static final Logger logger = LoggerFactory.getLogger(BitbucketServerController.class);

    private static final String OPEN = "OPEN";

    private static final String BITBUCKET_PREFIX = "bitbucket.";

    private static final int COMMENTS_SIZE = 2;

    private static final String BITBUCKET_SERVER_COMMENTS = "bitbucket.comments";

    private static final String PROJECT_KEY = "PROJECT_KEY";

    private static final String REPOSITORY_SLUG = "REPOSITORY_SLUG";

    private static final String PULL_REQUEST_ID = "PULL_REQUEST_ID";

    private static final String ACTION = "ACTION";

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

        logger.trace("Cards requests for bitbucket server connector - baseUrlHeader: {}, routingPrefix: {}",
                baseUrl,
                routingPrefix);

        final Set<String> cardTokens = cardRequest.getTokens(BITBUCKET_PR_EMAIL_SUBJECT);
        if (CollectionUtils.isEmpty(cardTokens)) {
            return Single.just(ResponseEntity.ok(new Cards()));
        }

        final Set<BitbucketServerPullRequest> pullRequests = convertToBitbucketServerPR(cardTokens);

        final HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, authHeader);

        return Observable.from(pullRequests)
                .flatMap(pullRequest -> getCardsForBitbucketServerPR(headers, pullRequest, baseUrl, routingPrefix))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .map(ResponseEntity::ok)
                .toSingle();
    }

    @PostMapping(
        path = "/api/v1/{projectKey}/{repositorySlug}/{pullRequestId}/approve",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> approve(final BitbucketServerPullRequest pullRequest,
                                                  @RequestHeader(AUTH_HEADER) final String authHeader,
                                                  @RequestHeader(BASE_URL_HEADER) final String baseUrl) {

        logger.debug("Approve ACTION for bitbucket server pull request: {}, baseURL: {}",
                pullRequest,
                baseUrl);

        return performBitbucketServerAction(baseUrl, authHeader, pullRequest, BitbucketServerAction.APPROVE);
    }

    @PostMapping(
            path = "/api/v1/{projectKey}/{repositorySlug}/{pullRequestId}/merge",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> merge(final BitbucketServerPullRequest pullRequest,
                                                  @RequestHeader(AUTH_HEADER) final String authHeader,
                                                  @RequestHeader(BASE_URL_HEADER) final String baseUrl) {

        logger.debug("Merge ACTION for bitbucket server pull request: {}, baseURL: {}",
                pullRequest,
                baseUrl);

        return performBitbucketServerAction(baseUrl, authHeader, pullRequest, BitbucketServerAction.MERGE);
    }

    @PostMapping(
            path = "/api/v1/{projectKey}/{repositorySlug}/{pullRequestId}/decline",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> decline(final BitbucketServerPullRequest pullRequest,
                                                @RequestHeader(AUTH_HEADER) final String authHeader,
                                                @RequestHeader(BASE_URL_HEADER) final String baseUrl) {

        logger.debug("Decline ACTION for bitbucket server pull request: {}, baseURL: {}",
                pullRequest,
                baseUrl);

        return performBitbucketServerAction(baseUrl, authHeader, pullRequest, BitbucketServerAction.DECLINE);
    }

    @PostMapping(
            path = "/api/v1/{projectKey}/{repositorySlug}/{pullRequestId}/comments",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> comments(final BitbucketServerPullRequest pullRequest,
                                                  @RequestParam(COMMENT_PARAM_KEY) final String comment,
                                                  @RequestHeader(AUTH_HEADER) final String authHeader,
                                                  @RequestHeader(BASE_URL_HEADER) final String baseUrl) {

        logger.info("Comment ACTION for bitbucket server pull request: {}, baseURL: {}",
                pullRequest,
                baseUrl);

        final HttpHeaders headers = buildHeaders(authHeader);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        final URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/rest/api/1.0/projects/{PROJECT_KEY}/repos/{REPOSITORY_SLUG}/pull-requests/{PULL_REQUEST_ID}/comments")
                .buildAndExpand(
                        ImmutableMap.of(
                                PROJECT_KEY, pullRequest.getProjectKey(),
                                REPOSITORY_SLUG, pullRequest.getRepositorySlug(),
                                PULL_REQUEST_ID, pullRequest.getPullRequestId()
                        )
                ).toUri();

        final BitbucketServerComment bitBucketServerComment = new BitbucketServerComment(comment);
        final ListenableFuture<ResponseEntity<String>> response = this.rest.exchange(
                uri,
                HttpMethod.POST,
                new HttpEntity<>(bitBucketServerComment, headers),
                String.class);
        return Async.toSingle(response);
    }

    @GetMapping("/test-auth")
    public Single<ResponseEntity<Void>> verifyAuth(@RequestHeader(AUTH_HEADER) final String authHeader,
                                                   @RequestHeader(BASE_URL_HEADER) final String baseUrl) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, authHeader);

        final ListenableFuture<ResponseEntity<JsonDocument>> response = this.rest.exchange(
                baseUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonDocument.class);

        return Async.toSingle(response)
                .map(BitbucketServerController::stripBody)
                .onErrorResumeNext(BitbucketServerController::map404To200);
    }

    private static ResponseEntity<Void> stripBody(final ResponseEntity<JsonDocument> entity) {
        return ResponseEntity.status(entity.getStatusCode()).build();
    }

    private static Single<ResponseEntity<Void>> map404To200(final Throwable throwable) {
        if (throwable instanceof HttpClientErrorException &&
                HttpClientErrorException.class.cast(throwable).getStatusCode() == HttpStatus.NOT_FOUND) {
            // If Bitbucket server base url is incorrect, then we return 200 back to the client.
            return Single.just(ResponseEntity.ok().build());
        }
        return Single.error(throwable);
    }

    private Single<List<String>> getComments(final String baseUrl,
                                     final HttpHeaders headers,
                                     final BitbucketServerPullRequest pullRequest) {

        final URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/rest/api/1.0/projects/{PROJECT_KEY}/repos/{REPOSITORY_SLUG}/pull-requests/{PULL_REQUEST_ID}/activities")
                .buildAndExpand(
                        ImmutableMap.of(
                                PROJECT_KEY, pullRequest.getProjectKey(),
                                REPOSITORY_SLUG, pullRequest.getRepositorySlug(),
                                PULL_REQUEST_ID, pullRequest.getPullRequestId()
                        )
                ).toUri();

        final ListenableFuture<ResponseEntity<JsonDocument>> response = this.rest.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonDocument.class);

        return Async.toSingle(response)
                .map(ResponseEntity::getBody)
                .map(jsonDocument -> jsonDocument.<List<String>>read("$.values[*].comment.text"))
                .map(comments -> comments.stream().limit(COMMENTS_SIZE).collect(Collectors.toList()));

    }

    private Single<ResponseEntity<String>> performBitbucketServerAction(final String baseUrl,
                                                                        final String authHeader,
                                                                        final BitbucketServerPullRequest pullRequest,
                                                                        final BitbucketServerAction bitBucketServerAction) {
        final HttpHeaders headers = buildHeaders(authHeader);

        // Get current version of the pull request. Pull request "version" changes when we do any actions on it.
        // When the pull request is raised, the current value will be 0.
        // For example, when we approve the pull request, then the version will change from 0 to 1.
        // We have to add the latest version of the pull request URI to do any actions. Otherwise, the ACTION will be rejected.
        // If the build for the branch is going on, then the actions would be rejected.
        // final Single<String> version = getVersion(headers, baseUrl, pullRequest);
        return getVersion(headers, baseUrl, pullRequest)
                .flatMap(version -> {

                    final URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/rest/api/1.0/projects/{PROJECT_KEY}/repos/{REPOSITORY_SLUG}/pull-requests/{PULL_REQUEST_ID}/{ACTION}")
                            .queryParam("version", version)
                            .buildAndExpand(
                                    ImmutableMap.of(
                                            PROJECT_KEY, pullRequest.getProjectKey(),
                                            REPOSITORY_SLUG, pullRequest.getRepositorySlug(),
                                            PULL_REQUEST_ID, pullRequest.getPullRequestId(),
                                            ACTION, bitBucketServerAction.getAction()
                                    )
                            )
                            .toUri();

                    // This header is added for skipping the CSRF check. Otherwise, Atlassian denies the pull request ACTION.
                    headers.add(ATLASSIAN_TOKEN, "no-check");

                    final ListenableFuture<ResponseEntity<String>> response = this.rest.exchange(
                            uri,
                            HttpMethod.POST,
                            new HttpEntity<>(headers),
                            String.class);

                    return Async.toSingle(response);
                });

    }

    private HttpHeaders buildHeaders(final String authHeader) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, authHeader);
        return headers;
    }

    private Single<String> getVersion(final HttpHeaders headers,
                              final String baseUrl,
                              final BitbucketServerPullRequest pullRequest) {

        final ListenableFuture<ResponseEntity<JsonDocument>> response = getPullRequestInfo(headers, pullRequest, baseUrl);
        return Async.toSingle(response)
                .map(entity -> entity.getBody())
                .map(jsonDocument -> Integer.toString(jsonDocument.read("$.version")));

    }

    private Observable<Card> getCardsForBitbucketServerPR(final HttpHeaders headers,
                                                          final BitbucketServerPullRequest pullRequest,
                                                          final String baseUrl,
                                                          final String routingPrefix) {
        logger.debug("Requesting pull request info from bitbucket server base url: {} and pull request info: {}", baseUrl, pullRequest);

        final Single<ResponseEntity<JsonDocument>> bitBucketServerResponse = Async.toSingle(getPullRequestInfo(headers, pullRequest, baseUrl));
        final Single<List<String>> comments = getComments(baseUrl, headers, pullRequest);

        return Observable.zip(bitBucketServerResponse.toObservable(), comments.toObservable(), Pair::of)
                .onErrorResumeNext(ObservableUtil::skip404)
                .map(pair -> convertResponseIntoCard(pair.getLeft(), pullRequest, routingPrefix, pair.getRight()));
    }

    private ListenableFuture<ResponseEntity<JsonDocument>> getPullRequestInfo(final HttpHeaders headers,
                                                                              final BitbucketServerPullRequest pullRequest,
                                                                              final String baseUrl) {
        final URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/rest/api/1.0/projects/{PROJECT_KEY}/repos/{REPOSITORY_SLUG}/pull-requests/{PULL_REQUEST_ID}")
                .buildAndExpand(
                        ImmutableMap.of(
                                PROJECT_KEY, pullRequest.getProjectKey(),
                                REPOSITORY_SLUG, pullRequest.getRepositorySlug(),
                                PULL_REQUEST_ID, pullRequest.getPullRequestId()
                        )
                ).toUri();

        return this.rest.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonDocument.class);
    }

    private Card convertResponseIntoCard(final ResponseEntity<JsonDocument> entity,
                                         final BitbucketServerPullRequest pullRequest,
                                         final String routingPrefix,
                                         final List<String> comments) {
        final JsonDocument bitBucketServerResponse = entity.getBody();

        final String author = bitBucketServerResponse.read("$.author.user.displayName");
        final String title = bitBucketServerResponse.read("$.title");
        final String description = bitBucketServerResponse.read("$.description");
        final boolean isPROpen = OPEN.equalsIgnoreCase(bitBucketServerResponse.read("$.state"));

        final Card.Builder card = new Card.Builder()
                .setHeader(
                        this.cardTextAccessor.getHeader(
                                title,
                                author),
                        this.cardTextAccessor.getMessage(
                                "bitbucket.card.subtitle",
                                description))
                .setBody(makeCardBody(bitBucketServerResponse, comments));

        addCommentAction(card, routingPrefix, pullRequest);

        // Add the following actions, only if the pull request state is open.
        if (isPROpen) {
            // Add approve ACTION.
            addPullRequestAction(card,
                    routingPrefix,
                    pullRequest,
                    BitbucketServerAction.APPROVE);

            // Add decline ACTION.
            addPullRequestAction(card,
                    routingPrefix,
                    pullRequest,
                    BitbucketServerAction.DECLINE);

            // Add merge ACTION.
            addPullRequestAction(card,
                    routingPrefix,
                    pullRequest,
                    BitbucketServerAction.MERGE);
        }

        return card.build();
    }

    private void addPullRequestAction(final Card.Builder card,
                                      final String routingPrefix,
                                      final BitbucketServerPullRequest pullRequest,
                                      final BitbucketServerAction bitBucketServerAction) {
        card.addAction(
                new CardAction.Builder()
                    .setLabel(this.cardTextAccessor.getActionLabel(BITBUCKET_PREFIX + bitBucketServerAction.getAction()))
                    .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel(BITBUCKET_PREFIX + bitBucketServerAction.getAction()))
                    .setActionKey(CardActionKey.DIRECT)
                    .setActionKey(buildActionUrl(routingPrefix, pullRequest, bitBucketServerAction))
                    .setType(HttpMethod.POST)
                    .build()
        );
    }

    private void addCommentAction(final Card.Builder card,
                                  final String routingPrefix,
                                  final BitbucketServerPullRequest pullRequest) {
        card.addAction(
                new CardAction.Builder()
                    .setLabel(this.cardTextAccessor.getActionLabel(BITBUCKET_SERVER_COMMENTS))
                    .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel(BITBUCKET_SERVER_COMMENTS))
                    .setActionKey(CardActionKey.USER_INPUT)
                    .setUrl(buildActionUrl(routingPrefix, pullRequest, BitbucketServerAction.COMMENTS))
                    .addUserInputField(
                            new CardActionInputField.Builder()
                                .setId("Comment")
                                .setLabel(this.cardTextAccessor.getMessage(BITBUCKET_SERVER_COMMENTS))
                                .setMinLength(1)
                                .build()
                    )
                    .setType(HttpMethod.POST)
                    .build()
                );
    }

    private String buildActionUrl(final String routingPrefix,
                                  final BitbucketServerPullRequest pullRequest,
                                  final BitbucketServerAction bitBucketServerAction) {

        return UriComponentsBuilder.fromHttpUrl(routingPrefix + "api/v1/{PROJECT_KEY}/{REPOSITORY_SLUG}/{PULL_REQUEST_ID}/{ACTION}")
                .buildAndExpand(
                        ImmutableMap.of(
                                PROJECT_KEY, pullRequest.getProjectKey(),
                                REPOSITORY_SLUG, pullRequest.getRepositorySlug(),
                                PULL_REQUEST_ID, pullRequest.getPullRequestId(),
                                ACTION, bitBucketServerAction.getAction()
                        )
                ).toUri().toString();
    }

    private CardBody makeCardBody(final JsonDocument bitbucketServerResponse, final List<String> comments) {
        final CardBody.Builder cardBody = new CardBody.Builder();
        cardBody.setDescription(bitbucketServerResponse.read("$.description"));

        cardBody.addField(makeCardBodyField("bitbucket.repository", bitbucketServerResponse.read("$.fromRef.repository.slug")));
        cardBody.addField(makeCardBodyField("bitbucket.author", bitbucketServerResponse.read("$.author.user.displayName")));
        cardBody.addField(makeCardBodyField("bitbucket.title", bitbucketServerResponse.read("$.title")));
        cardBody.addField(makeCardBodyField("bitbucket.state", bitbucketServerResponse.read("$.state")));
        cardBody.addField(makeCardBodyField("bitbucket.open", Boolean.toString(bitbucketServerResponse.read("$.open"))));

        if (!CollectionUtils.isEmpty(comments)) {
            cardBody.addField(addCommentField(comments));
        }
        return cardBody.build();
    }

    private CardBodyField addCommentField(final List<String> comments) {
        final CardBodyField.Builder cardBodyFieldBuilder = new CardBodyField.Builder();
        cardBodyFieldBuilder.setTitle(this.cardTextAccessor.getMessage(BITBUCKET_SERVER_COMMENTS));
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

    private Set<BitbucketServerPullRequest> convertToBitbucketServerPR(final Set<String> cardTokens) {
        final Set<BitbucketServerPullRequest> pullRequests = new HashSet<>();
        final Pattern pattern = Pattern.compile(BITBUCKET_PR_EMAIL_SUBJECT_REGEX);

        for (final String prEmailSubject: cardTokens) {
            final Matcher matcher = pattern.matcher(prEmailSubject);
            while(matcher.find()) {
                final BitbucketServerPullRequest pullRequest = new BitbucketServerPullRequest(
                        matcher.group(2),
                        matcher.group(3),
                        matcher.group(4));
                pullRequests.add(pullRequest);
            }
        }
        return pullRequests;
    }
}
