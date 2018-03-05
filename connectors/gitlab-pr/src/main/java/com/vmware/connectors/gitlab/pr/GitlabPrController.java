/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.gitlab.pr;

import com.google.common.collect.ImmutableMap;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.Reactive;
import com.vmware.connectors.gitlab.pr.v4.MergeRequest;
import com.vmware.connectors.gitlab.pr.v4.MergeRequestActionConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestController
public class GitlabPrController {

    private static final Logger logger = LoggerFactory.getLogger(GitlabPrController.class);

    private static final String AUTH_HEADER = "x-gitlab-pr-authorization";
    private static final String BASE_URL_HEADER = "x-gitlab-pr-base-url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";

    private static final String CLOSE_REASON_PARAM_KEY = "reason";
    private static final String COMMENT_PARAM_KEY = "message";
    private static final String SHA_PARAM_KEY = "sha";

    // Number of path segments expected in our pull request urls
    private static final int NUM_PR_PATH_SEGMENTS = 4;

    // Merge Request path name expected
    private static final String MERGE_REQUESTS = "merge_requests";

    private final boolean isEnterpriseEdition;
    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public GitlabPrController(
            @Value("${gitlab.connector.enterprise:false}") boolean isEnterpriseEdition,
            WebClient rest,
            CardTextAccessor cardTextAccessor
    ) {
        this.isEnterpriseEdition = isEnterpriseEdition;
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

        Stream<MergeRequestId> mergeRequestIds = request.getTokens("merge_request_urls")
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
        return uriComponents.getHost().equalsIgnoreCase("gitlab.com");
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

    private Flux<Pair<MergeRequestId, MergeRequest>> fetchMergeRequest(
            String baseUrl,
            MergeRequestId mergeRequestId,
            String auth
    ) {
        logger.trace("fetchMergeRequest called: baseUrl={}, id={}", baseUrl, mergeRequestId);

        return rest.get()
                .uri(makeGitlabUri(baseUrl, mergeRequestId))
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToFlux(MergeRequest.class)
                .onErrorResume(throwable -> Reactive.skipOnStatus(throwable, NOT_FOUND))
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
                    baseUrl + "/api/v4/projects/" + mergeRequestId.getProjectId()
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
            Locale locale
    ) {
        logger.trace("makeCard called: routingPrefix={}, info={}", routingPrefix, info);

        MergeRequestId mergeRequestId = info.getLeft();
        MergeRequest mergeRequest = info.getRight();

        Card.Builder card = new Card.Builder()
                .setName("GitlabPr") // TODO - remove this in APF-536
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(
                        cardTextAccessor.getHeader(locale),
                        cardTextAccessor.getMessage(
                                "subtitle", locale,
                                mergeRequestId.getNamespace(),
                                mergeRequestId.getProjectName(),
                                mergeRequestId.getNumber()
                        )
                )
                .setBody(createBody(mergeRequestId, mergeRequest, locale));

        addCloseAction(card, routingPrefix, mergeRequestId, mergeRequest, locale);
        addMergeAction(card, routingPrefix, mergeRequestId, mergeRequest, locale);
        addApproveAction(card, routingPrefix, mergeRequestId, mergeRequest, locale);
        addCommentAction(card, routingPrefix, mergeRequestId, locale);

        return card.build();
    }

    private CardBody createBody(
            MergeRequestId mergeRequestId,
            MergeRequest mergeRequest,
            Locale locale
    ) {
        CardBody.Builder body = new CardBody.Builder();

        addInfo(body, mergeRequestId, mergeRequest, locale);
        addChangeStats(body, mergeRequest, locale);

        return body.build();
    }

    private void addInfo(
            CardBody.Builder body,
            MergeRequestId mergeRequestId,
            MergeRequest mergeRequest,
            Locale locale
    ) {
        body
                .setDescription(cardTextAccessor.getBody(locale, mergeRequest.getDescription()))
                .addField(buildGeneralBodyField("repository", locale, mergeRequestId.getNamespace(), mergeRequestId.getProjectName()))
                .addField(buildGeneralBodyField("requester", locale, mergeRequest.getAuthor().getUsername()))
                .addField(buildGeneralBodyField("title", locale, mergeRequest.getTitle()))
                .addField(buildGeneralBodyField("state", locale, mergeRequest.getState()))
                .addField(buildGeneralBodyField("mergeable", locale, mergeRequest.getMergeStatus()))
                .addField(
                        buildGeneralBodyField(
                                "createdAt", locale,
                                DateTimeFormatter.ISO_INSTANT.format(mergeRequest.getCreatedAt().toInstant())
                        )
                )
                .addField(buildGeneralBodyField("comments", locale, mergeRequest.getUserNotesCount()));
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

    private void addChangeStats(
            CardBody.Builder body,
            MergeRequest mergeRequest,
            Locale locale
    ) {
        body.addField(buildGeneralBodyField("changes", locale, mergeRequest.getChangesCount()));
    }

    private void addCloseAction(
            Card.Builder card,
            String routingPrefix,
            MergeRequestId mergeRequestId,
            MergeRequest mergeRequest,
            Locale locale
    ) {
        if (mergeRequest.getState().isOpen()) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("close", locale))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("close", locale))
                            .setActionKey(CardActionKey.USER_INPUT)
                            .setUrl(getActionUrl(routingPrefix, mergeRequestId, "close"))
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
            MergeRequestId mergeRequestId,
            MergeRequest mergeRequest,
            Locale locale
    ) {
        if (mergeRequest.getState().isOpen() && mergeRequest.getMergeStatus().canBeMerged()) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("merge", locale))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("merge", locale))
                            .setActionKey(CardActionKey.DIRECT)
                            .setUrl(getActionUrl(routingPrefix, mergeRequestId, "merge"))
                            .addRequestParam(SHA_PARAM_KEY, mergeRequest.getSha())
                            .setType(HttpMethod.POST)
                            .build()
            );
        }
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
            path = "/api/v1/{namespace}/{projectName}/{number}/close",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<String> close(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            MergeRequestId mergeRequestId,
            @RequestParam(name = CLOSE_REASON_PARAM_KEY, required = false) String reason
    ) {
        logger.trace(
                "close called: baseUrl={}, id={}, reason={}",
                baseUrl,
                mergeRequestId,
                reason
        );

        Mono<String> noteResponse;

        if (StringUtils.isEmpty(reason)) {
            noteResponse = Mono.just("does not matter");
        } else {
            noteResponse = postNote(
                    auth,
                    baseUrl,
                    mergeRequestId,
                    reason
            );
        }

        return noteResponse
                .flatMap(ignored -> closeMergeRequest(auth, baseUrl, mergeRequestId));
    }

    private Mono<String> closeMergeRequest(
            String auth,
            String baseUrl,
            MergeRequestId mergeRequestId
    ) {
        Map<String, Object> body = ImmutableMap.of(
                MergeRequestActionConstants.Properties.STATE_EVENT, MergeRequestActionConstants.StateEvent.close
        );

        return actionRequest(auth, baseUrl, mergeRequestId, "", HttpMethod.PUT, body);
    }

    @PostMapping(
            path ="/api/v1/{namespace}/{projectName}/{number}/merge",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<String> merge(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            MergeRequestId mergeRequestId,
            @RequestParam(SHA_PARAM_KEY) String sha
    ) {
        logger.trace(
                "merge called: baseUrl={}, id={}, sha={}",
                baseUrl,
                mergeRequestId,
                sha
        );

        Map<String, Object> body = ImmutableMap.of(
                MergeRequestActionConstants.Properties.SHA, sha
        );

        return actionRequest(auth, baseUrl, mergeRequestId, "/merge", HttpMethod.PUT, body);
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
