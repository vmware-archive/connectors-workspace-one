/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.bitbucket.server;

import com.vmware.connectors.bitbucket.server.utils.BitbucketServerPullRequest;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.CommonUtils;
import com.vmware.connectors.common.utils.Reactive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
public class BitbucketServerController {

    private static final Logger logger = LoggerFactory.getLogger(BitbucketServerController.class);

    private static final String AUTH_HEADER = "X-Connector-Authorization";
    private static final String BASE_URL_HEADER = "X-Connector-Base-Url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";

    private static final String PROJECT_PARAM_KEY = "project";
    private static final String USER_PARAM_KEY = "user";
    private static final String COMMENT_PARAM_KEY = "comment";

    private static final String OPEN = "OPEN";

    // To prevent CSRF check by Bitbucket Server.
    private static final String ATLASSIAN_TOKEN = "X-Atlassian-Token";

    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public BitbucketServerController(WebClient rest, CardTextAccessor cardTextAccessor) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
    }

    @GetMapping("/test-auth")
    public Mono<Void> verifyAuth(
            @RequestHeader(AUTH_HEADER) String authHeader,
            @RequestHeader(BASE_URL_HEADER) String baseUrl
    ) {
        return rest.head()
                .uri(baseUrl + "/rest/api/1.0/dashboard/pull-request-suggestions?limit=1")
                .header(AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(Void.class);
    }

    @PostMapping(
            value = "/cards/requests",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<Cards> getCards(
            @RequestHeader(AUTH_HEADER) String authHeader,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            Locale locale,
            @Valid @RequestBody CardRequest cardRequest,
            HttpServletRequest request
    ) {
        logger.debug("Cards requests for bitbucket server connector - baseUrlHeader: {}, routingPrefix: {}", baseUrl, routingPrefix);

        Set<String> projectRepoTokens = cardRequest.getTokens("projects_pr_email_subject");
        Set<String> userRepoTokens = cardRequest.getTokens("users_pr_email_subject");

        Set<BitbucketServerPullRequest> pullRequests = convertToProjectRepoPr(projectRepoTokens);
        pullRequests.addAll(convertToUserRepoPr(userRepoTokens));

        return Flux.fromIterable(pullRequests)
                .flatMap(pullRequest -> getCardForPr(authHeader, pullRequest, baseUrl, routingPrefix, locale, request))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .defaultIfEmpty(new Cards())
                .subscriberContext(Reactive.setupContext());
    }

    private Set<BitbucketServerPullRequest> convertToProjectRepoPr(
            Set<String> cardTokens
    ) {
        return convertToPrs(
                cardTokens,
                matcher -> new BitbucketServerPullRequest(null, matcher.group(2), matcher.group(3), matcher.group(4))
        );
    }

    private Set<BitbucketServerPullRequest> convertToUserRepoPr(
            Set<String> cardTokens
    ) {
        return convertToPrs(
                cardTokens,
                matcher -> new BitbucketServerPullRequest(matcher.group(2), null, matcher.group(3), matcher.group(4))
        );
    }

    private Set<BitbucketServerPullRequest> convertToPrs(
            Set<String> cardTokens,
            Function<Matcher, BitbucketServerPullRequest> mapper
    ) {
        Set<BitbucketServerPullRequest> pullRequests = new HashSet<>();

        if (cardTokens == null) {
            return pullRequests;
        }

        String regex = "(([a-zA-Z0-9]+)\\/([a-zA-Z0-9-]+) - Pull request #([0-9]+):[ ])";
        Pattern pattern = Pattern.compile(regex);

        for (String prEmailSubject : cardTokens) {
            Matcher matcher = pattern.matcher(prEmailSubject);
            while (matcher.find()) {
                pullRequests.add(mapper.apply(matcher));
            }
        }

        return pullRequests;
    }

    private Mono<Card> getCardForPr(
            String authHeader,
            BitbucketServerPullRequest pullRequest,
            String baseUrl,
            String routingPrefix,
            Locale locale,
            HttpServletRequest request
    ) {
        logger.debug("Requesting pull request info from bitbucket server base url: {} and pull request info: {}", baseUrl, pullRequest);

        Mono<JsonDocument> response = getPullRequestInfo(authHeader, pullRequest, baseUrl);

        return response
                .onErrorResume(Reactive::skipOnNotFound)
                .map(prResponse -> convertResponseIntoCard(prResponse, pullRequest, routingPrefix, locale, request));
    }

    private Mono<JsonDocument> getPullRequestInfo(
            String authHeader,
            BitbucketServerPullRequest pullRequest,
            String baseUrl
    ) {
        return rest.get()
                .uri(
                        baseUrl + "/rest/api/1.0/{prefix}/{key}/repos/{respositorySlug}/pull-requests/{pullRequestId}",
                        getPrefix(pullRequest),
                        getKey(pullRequest),
                        pullRequest.getRepositorySlug(),
                        pullRequest.getPullRequestId()
                )
                .header(AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(JsonDocument.class);
    }

    private String getPrefix(BitbucketServerPullRequest pullRequest) {
        return pullRequest.isProject() ? "projects" : "users";
    }

    private String getKey(BitbucketServerPullRequest pullRequest) {
        return pullRequest.isProject() ? pullRequest.getProjectKey() : pullRequest.getUserKey();
    }

    private Card convertResponseIntoCard(
            JsonDocument response,
            BitbucketServerPullRequest pullRequest,
            String routingPrefix,
            Locale locale,
            HttpServletRequest request
    ) {
        Card.Builder card = new Card.Builder()
                .setHeader(
                        cardTextAccessor.getHeader(locale,
                                pullRequest.getPullRequestId(),
                                response.read("$.author.user.displayName")
                        ),
                        cardTextAccessor.getMessage("subtitle", locale,
                                getKey(pullRequest),
                                pullRequest.getRepositorySlug()
                        )
                );

        // Set image url to card response.
        CommonUtils.buildConnectorImageUrl(card, request);

        if (OPEN.equalsIgnoreCase(response.read("$.state"))) {
            card.addAction(
                    new CardAction.Builder()
                            .setPrimary(true)
                            .setLabel(cardTextAccessor.getActionLabel("bitbucket.approve", locale))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("bitbucket.approve", locale))
                            .setActionKey(CardActionKey.DIRECT)
                            .addRequestParam(USER_PARAM_KEY, pullRequest.getUserKey())
                            .addRequestParam(PROJECT_PARAM_KEY, pullRequest.getProjectKey())
                            .setUrl(buildActionUrl(routingPrefix, pullRequest, "approve"))
                            .setType(HttpMethod.POST)
                            .build()
            );
        }

        card.addAction(
                new CardAction.Builder()
                        .setLabel(cardTextAccessor.getActionLabel("bitbucket.comments", locale))
                        .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("bitbucket.comments", locale))
                        .setActionKey(CardActionKey.USER_INPUT)
                        .addRequestParam(USER_PARAM_KEY, pullRequest.getUserKey())
                        .addRequestParam(PROJECT_PARAM_KEY, pullRequest.getProjectKey())
                        .setUrl(buildActionUrl(routingPrefix, pullRequest, "comments"))
                        .setAllowRepeated(true)
                        .addUserInputField(
                                new CardActionInputField.Builder()
                                        .setId(COMMENT_PARAM_KEY)
                                        .setFormat("textarea")
                                        .setLabel(cardTextAccessor.getMessage("bitbucket.comments", locale))
                                        .setMinLength(1)
                                        .build()
                        )
                        .setType(HttpMethod.POST)
                        .build()
        );

        return card.build();
    }

    private String buildActionUrl(
            String routingPrefix,
            BitbucketServerPullRequest pullRequest,
            String action
    ) {
        return String.format(
                "%sapi/v1/%s/%s/%s",
                routingPrefix,
                pullRequest.getRepositorySlug(),
                pullRequest.getPullRequestId(),
                action
        );
    }

    @PostMapping(
            path = "/api/v1/{repositorySlug}/{pullRequestId}/approve",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<String> approve(
            @RequestParam(name = USER_PARAM_KEY, required = false) String user,
            @RequestParam(name = PROJECT_PARAM_KEY, required = false) String project,
            @PathVariable("repositorySlug") String repositorySlug,
            @PathVariable("pullRequestId") String pullRequestId,
            @RequestHeader(AUTH_HEADER) String authHeader,
            @RequestHeader(BASE_URL_HEADER) String baseUrl
    ) {
        BitbucketServerPullRequest pullRequest = new BitbucketServerPullRequest(
                user,
                project,
                repositorySlug,
                pullRequestId
        );

        logger.debug("Approve ACTION for bitbucket server pull request: {}, baseURL: {}", pullRequest, baseUrl);

        return forceApprove(baseUrl, authHeader, pullRequest, "approve");
    }

    private Mono<String> forceApprove(
            String baseUrl,
            String authHeader,
            BitbucketServerPullRequest pullRequest,
            String action
    ) {
        // Get current version of the pull request. Pull request "version" changes when we do any actions on it.
        // When the pull request is raised, the current value will be 0.
        // For example, when we approve the pull request, then the version will change from 0 to 1.
        // We have to add the latest version of the pull request URI to do any actions. Otherwise, the ACTION will be rejected.
        // If the build for the branch is going on, then the actions would be rejected.
        return getVersion(authHeader, baseUrl, pullRequest)
                .flatMap(version -> callApprove(baseUrl, authHeader, pullRequest, action, version));
    }

    private Mono<Integer> getVersion(
            String authHeader,
            String baseUrl,
            BitbucketServerPullRequest pullRequest
    ) {
        return getPullRequestInfo(authHeader, pullRequest, baseUrl)
                .map(jsonDocument -> jsonDocument.read("$.version"));
    }

    private Mono<String> callApprove(
            String baseUrl,
            String authHeader,
            BitbucketServerPullRequest pullRequest,
            String action,
            Integer version
    ) {
        return rest.post()
                .uri(
                        baseUrl + "/rest/api/1.0/{prefix}/{key}/repos/{repositoryPlug}/pull-requests/{pullRequestId}/{action}?version={version}",
                        getPrefix(pullRequest),
                        getKey(pullRequest),
                        pullRequest.getRepositorySlug(),
                        pullRequest.getPullRequestId(),
                        action,
                        version
                )
                .header(AUTHORIZATION, authHeader)
                .header(ATLASSIAN_TOKEN, "no-check")
                .retrieve()
                .bodyToMono(String.class);
    }

    @PostMapping(
            path = "/api/v1/{repositorySlug}/{pullRequestId}/comments",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<ResponseEntity<String>> comments(
            @RequestParam(name = USER_PARAM_KEY, required = false) String user,
            @RequestParam(name = PROJECT_PARAM_KEY, required = false) String project,
            @PathVariable("repositorySlug") String repositorySlug,
            @PathVariable("pullRequestId") String pullRequestId,
            @RequestParam(COMMENT_PARAM_KEY) String comment,
            @RequestHeader(AUTH_HEADER) String authHeader,
            @RequestHeader(BASE_URL_HEADER) String baseUrl
    ) {
        BitbucketServerPullRequest pullRequest = new BitbucketServerPullRequest(
                user,
                project,
                repositorySlug,
                pullRequestId
        );

        logger.debug("Comment ACTION for bitbucket server pull request: {}, baseURL: {}", pullRequest, baseUrl);

        Map<String, String> payload = Map.of("text", comment);

        return rest.post()
                .uri(
                        baseUrl + "/rest/api/1.0/{prefix}/{key}/repos/{repositorySlug}/pull-requests/{pullRequestId}/comments",
                        getPrefix(pullRequest),
                        getKey(pullRequest),
                        pullRequest.getRepositorySlug(),
                        pullRequest.getPullRequestId()
                )
                .header(AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody(payload)
                .exchange()
                .flatMap(Reactive::checkStatus)
                .flatMap(response -> response.toEntity(String.class));
    }

}
