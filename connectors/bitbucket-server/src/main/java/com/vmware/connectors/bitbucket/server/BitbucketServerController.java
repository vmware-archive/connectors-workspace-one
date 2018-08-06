/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.bitbucket.server;

import com.vmware.connectors.bitbucket.server.utils.BitbucketServerAction;
import com.vmware.connectors.bitbucket.server.utils.BitbucketServerComment;
import com.vmware.connectors.bitbucket.server.utils.BitbucketServerPullRequest;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.CommonUtils;
import com.vmware.connectors.common.utils.Reactive;
import org.apache.commons.io.IOUtils;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.connectors.bitbucket.server.utils.BitbucketServerConstants.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
public class BitbucketServerController {

    private static final Logger logger = LoggerFactory.getLogger(BitbucketServerController.class);

    private static final String OPEN = "OPEN";

    private static final String BITBUCKET_PREFIX = "bitbucket.";

    private static final String BITBUCKET_SERVER_COMMENTS = "bitbucket.comments";

    private final WebClient rest;
    private final String metadata;
    private final CardTextAccessor cardTextAccessor;
    private final long maxAge;
    private final TimeUnit unit;

    @Autowired
    public BitbucketServerController(WebClient rest, CardTextAccessor cardTextAccessor,
                                     @Value("classpath:static/discovery/metadata.json") Resource metadataJsonResource,
                                     @Value("${rootDiscovery.cacheControl.maxAge:1}") long maxAge,
                                     @Value("${rootDiscovery.cacheControl.unit:HOURS}") TimeUnit unit) throws IOException {
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
            value = "/cards/requests",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<Cards> getCards(
            @RequestHeader(AUTH_HEADER) final String authHeader,
            @RequestHeader(BASE_URL_HEADER) final String baseUrl,
            @RequestHeader(ROUTING_PREFIX) final String routingPrefix,
            final Locale locale,
            @Valid @RequestBody final CardRequest cardRequest,
            final HttpServletRequest request) {

        logger.trace("Cards requests for bitbucket server connector - baseUrlHeader: {}, routingPrefix: {}",
                baseUrl,
                routingPrefix);

        final Set<String> cardTokens = cardRequest.getTokens(BITBUCKET_PR_EMAIL_SUBJECT);

        final Set<BitbucketServerPullRequest> pullRequests = convertToBitbucketServerPR(cardTokens);

        return Flux.fromIterable(pullRequests)
                .flatMap(pullRequest -> getCardForBitbucketServerPR(authHeader, pullRequest, baseUrl, routingPrefix, locale, request))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .defaultIfEmpty(new Cards())
                .subscriberContext(Reactive.setupContext());
    }

    @PostMapping(
            path = "/api/v1/{projectKey}/{repositorySlug}/{pullRequestId}/approve",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<String> approve(final BitbucketServerPullRequest pullRequest,
                                @RequestHeader(AUTH_HEADER) final String authHeader,
                                @RequestHeader(BASE_URL_HEADER) final String baseUrl) {

        logger.debug("Approve ACTION for bitbucket server pull request: {}, baseURL: {}",
                pullRequest,
                baseUrl);

        return performBitbucketServerAction(baseUrl, authHeader, pullRequest, BitbucketServerAction.APPROVE);
    }

    @PostMapping(
            path = "/api/v1/{projectKey}/{repositorySlug}/{pullRequestId}/comments",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<ResponseEntity<String>> comments(final BitbucketServerPullRequest pullRequest,
                                                 @RequestParam(COMMENT_PARAM_KEY) final String comment,
                                                 @RequestHeader(AUTH_HEADER) final String authHeader,
                                                 @RequestHeader(BASE_URL_HEADER) final String baseUrl) {

        logger.info("Comment ACTION for bitbucket server pull request: {}, baseURL: {}",
                pullRequest,
                baseUrl);

        final BitbucketServerComment bitBucketServerComment = new BitbucketServerComment(comment);
        return rest.post()
                .uri(baseUrl + "/rest/api/1.0/projects/{projectKey}/repos/{repositorySlug}/pull-requests/{pullRequestId}/comments",
                        pullRequest.getProjectKey(), pullRequest.getRepositorySlug(), pullRequest.getPullRequestId())
                .header(AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody(bitBucketServerComment)
                .exchange()
                .flatMap(Reactive::checkStatus)
                .flatMap(response -> response.toEntity(String.class));
    }

    @GetMapping("/test-auth")
    public Mono<ResponseEntity<Void>> verifyAuth(@RequestHeader(AUTH_HEADER) final String authHeader,
                                                 @RequestHeader(BASE_URL_HEADER) final String baseUrl) {
        return rest.head()
                .uri(baseUrl + "/rest/api/1.0/dashboard/pull-request-suggestions?limit=1")
                .header(AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(Void.class)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    private Mono<String> performBitbucketServerAction(final String baseUrl,
                                                      final String authHeader,
                                                      final BitbucketServerPullRequest pullRequest,
                                                      final BitbucketServerAction bitBucketServerAction) {
        // Get current version of the pull request. Pull request "version" changes when we do any actions on it.
        // When the pull request is raised, the current value will be 0.
        // For example, when we approve the pull request, then the version will change from 0 to 1.
        // We have to add the latest version of the pull request URI to do any actions. Otherwise, the ACTION will be rejected.
        // If the build for the branch is going on, then the actions would be rejected.
        return getVersion(authHeader, baseUrl, pullRequest)
                .flatMap(version -> performBitbucketServerAction(baseUrl,
                        authHeader, pullRequest, bitBucketServerAction, version));
    }

    private Mono<String> performBitbucketServerAction(final String baseUrl,
                                                      final String authHeader,
                                                      final BitbucketServerPullRequest pullRequest,
                                                      final BitbucketServerAction bitBucketServerAction,
                                                      final String version) {
        return rest.post()
                .uri(baseUrl + "/rest/api/1.0/projects/{projectKey}/repos/{repositoryPlug}/pull-requests/{pullRequestId}/{action}?version={version}",
                        pullRequest.getProjectKey(),
                        pullRequest.getRepositorySlug(),
                        pullRequest.getPullRequestId(),
                        bitBucketServerAction.getAction(),
                        version)
                .header(AUTHORIZATION, authHeader)
                .header(ATLASSIAN_TOKEN, "no-check")
                .retrieve()
                .bodyToMono(String.class);
    }

    private Mono<String> getVersion(final String authHeader,
                                    final String baseUrl,
                                    final BitbucketServerPullRequest pullRequest) {

        return getPullRequestInfo(authHeader, pullRequest, baseUrl)
               .map(jsonDocument -> Integer.toString(jsonDocument.read("$.version")));

    }

    private Mono<Card> getCardForBitbucketServerPR(final String authHeader,
                                                   final BitbucketServerPullRequest pullRequest,
                                                   final String baseUrl,
                                                   final String routingPrefix,
                                                   final Locale locale,
                                                   final HttpServletRequest request) {
        logger.debug("Requesting pull request info from bitbucket server base url: {} and pull request info: {}", baseUrl, pullRequest);

        final Mono<JsonDocument> bitBucketServerResponse = getPullRequestInfo(authHeader, pullRequest, baseUrl);

        return bitBucketServerResponse
                .onErrorResume(Reactive::skipOnNotFound)
                .map(prResponse -> convertResponseIntoCard(prResponse, pullRequest, routingPrefix, locale, request));
    }

    private Mono<JsonDocument> getPullRequestInfo(final String authHeader,
                                                                    final BitbucketServerPullRequest pullRequest,
                                                                    final String baseUrl) {
        return rest.get()
                .uri(baseUrl+ "/rest/api/1.0/projects/{projectKey}/repos/{respositorySlug}/pull-requests/{pullRequestId}",
                        pullRequest.getProjectKey(), pullRequest.getRepositorySlug(), pullRequest.getPullRequestId())
                .header(AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(JsonDocument.class);
    }

    private Card convertResponseIntoCard(final JsonDocument bitBucketServerResponse,
                                         final BitbucketServerPullRequest pullRequest,
                                         final String routingPrefix,
                                         final Locale locale,
                                         final HttpServletRequest request) {
        final boolean isPROpen = OPEN.equalsIgnoreCase(bitBucketServerResponse.read("$.state"));

        final Card.Builder card = new Card.Builder()
                .setHeader(
                        this.cardTextAccessor.getHeader(locale,
                                pullRequest.getPullRequestId(),
                                bitBucketServerResponse.read("$.author.user.displayName")),
                        this.cardTextAccessor.getMessage("subtitle", locale,
                                pullRequest.getProjectKey(),
                                pullRequest.getRepositorySlug())
                );

        // Set image url to card response.
        CommonUtils.buildConnectorImageUrl(card, request);

        // Add the following actions, only if the pull request state is open.
        if (isPROpen) {
            // Add approve ACTION.
            addPullRequestAction(card,
                    routingPrefix,
                    pullRequest,
                    BitbucketServerAction.APPROVE,
                    locale);
        }

        // Add comment action.
        addCommentAction(card, routingPrefix, pullRequest, locale);

        return card.build();
    }

    private void addPullRequestAction(final Card.Builder card,
                                      final String routingPrefix,
                                      final BitbucketServerPullRequest pullRequest,
                                      final BitbucketServerAction bitBucketServerAction,
                                      final Locale locale) {
        card.addAction(
                new CardAction.Builder()
                        .setLabel(this.cardTextAccessor.getActionLabel(BITBUCKET_PREFIX + bitBucketServerAction.getAction(), locale))
                        .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel(BITBUCKET_PREFIX + bitBucketServerAction.getAction(), locale))
                        .setActionKey(CardActionKey.DIRECT)
                        .setUrl(buildActionUrl(routingPrefix, pullRequest, bitBucketServerAction))
                        .setType(HttpMethod.POST)
                        .build()
        );
    }

    private void addCommentAction(final Card.Builder card,
                                  final String routingPrefix,
                                  final BitbucketServerPullRequest pullRequest,
                                  final Locale locale) {
        card.addAction(
                new CardAction.Builder()
                        .setLabel(this.cardTextAccessor.getActionLabel(BITBUCKET_SERVER_COMMENTS, locale))
                        .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel(BITBUCKET_SERVER_COMMENTS, locale))
                        .setActionKey(CardActionKey.USER_INPUT)
                        .setUrl(buildActionUrl(routingPrefix, pullRequest, BitbucketServerAction.COMMENTS))
                        .setAllowRepeated(true)
                        .addUserInputField(
                                new CardActionInputField.Builder()
                                        .setId(COMMENT_PARAM_KEY)
                                        .setFormat("textarea")
                                        .setLabel(this.cardTextAccessor.getMessage(BITBUCKET_SERVER_COMMENTS, locale))
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

        return String.format("%sapi/v1/%s/%s/%s/%s",
                routingPrefix,
                pullRequest.getProjectKey(),
                pullRequest.getRepositorySlug(),
                pullRequest.getPullRequestId(),
                bitBucketServerAction.getAction());
    }

    private Set<BitbucketServerPullRequest> convertToBitbucketServerPR(final Set<String> cardTokens) {
        final Set<BitbucketServerPullRequest> pullRequests = new HashSet<>();
        final Pattern pattern = Pattern.compile(BITBUCKET_PR_EMAIL_SUBJECT_REGEX);

        for (final String prEmailSubject : cardTokens) {
            final Matcher matcher = pattern.matcher(prEmailSubject);
            while (matcher.find()) {
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
