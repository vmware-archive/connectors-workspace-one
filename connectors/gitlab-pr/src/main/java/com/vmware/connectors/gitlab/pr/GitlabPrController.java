/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.gitlab.pr;

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
import com.vmware.connectors.gitlab.pr.v4.MergeRequest;
import com.vmware.connectors.gitlab.pr.v4.MergeRequestActionConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final AsyncRestOperations rest;
    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public GitlabPrController(
            @Value("${gitlab.connector.enterprise:false}") boolean isEnterpriseEdition,
            AsyncRestOperations rest,
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
    public Single<ResponseEntity<Cards>> getCards(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            @Valid @RequestBody CardRequest request
    ) {
        logger.trace("getCards called: baseUrl={}, routingPrefix={}, request={}", baseUrl, routingPrefix, request);

        List<MergeRequestId> mergeRequestIds = request.getTokens("merge_request_urls")
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()) // squash duplicates
                .stream()
                .sorted()
                .map(this::parseUri)
                .filter(Objects::nonNull)
                .filter(this::validHost)
                .map(this::getMergeRequestId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(mergeRequestIds)) {
            return Single.just(ResponseEntity.ok(new Cards()));
        }

        HttpHeaders headers = makeHeaders(auth);
        HttpEntity<HttpHeaders> httpHeaders = new HttpEntity<>(headers);

        return fetchAllMergeRequests(baseUrl, httpHeaders, mergeRequestIds)
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

    private HttpHeaders makeHeaders(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, auth);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    private Observable<Pair<MergeRequestId, MergeRequest>> fetchAllMergeRequests(
            String baseUrl,
            HttpEntity<HttpHeaders> headers,
            List<MergeRequestId> mergeRequestIds
    ) {
        logger.trace("fetchAllMergeRequests called: baseUrl={}, ids={}", baseUrl, mergeRequestIds);

        return Observable.from(mergeRequestIds)
                .flatMap(mergeRequestId -> fetchMergeRequest(baseUrl, mergeRequestId, headers));
    }

    private Observable<Pair<MergeRequestId, MergeRequest>> fetchMergeRequest(
            String baseUrl,
            MergeRequestId mergeRequestId,
            HttpEntity<HttpHeaders> headers
    ) {
        logger.trace("fetchMergeRequest called: baseUrl={}, id={}", baseUrl, mergeRequestId);

        ListenableFuture<ResponseEntity<MergeRequest>> response = rest.exchange(
                makeGitlabUri(baseUrl, mergeRequestId),
                HttpMethod.GET,
                headers,
                MergeRequest.class
        );

        return Async.toSingle(response)
                .toObservable()
                .onErrorResumeNext(ObservableUtil::skip404)
                .map(ResponseEntity::getBody)
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
            Pair<MergeRequestId, MergeRequest> info
    ) {
        logger.trace("makeCard called: routingPrefix={}, info={}", routingPrefix, info);

        MergeRequestId mergeRequestId = info.getLeft();
        MergeRequest mergeRequest = info.getRight();

        Card.Builder card = new Card.Builder()
                .setName("GitlabPr") // TODO - remove this in APF-536
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(
                        cardTextAccessor.getHeader(
                                mergeRequestId.getNamespace(),
                                mergeRequestId.getProjectName(),
                                mergeRequestId.getNumber()
                        ),
                        cardTextAccessor.getMessage(
                                "subtitle",
                                mergeRequestId.getNamespace(),
                                mergeRequestId.getProjectName(),
                                mergeRequestId.getNumber()
                        )
                )
                .setBody(createBody(mergeRequestId, mergeRequest));

        addCloseAction(card, routingPrefix, mergeRequestId, mergeRequest);
        addMergeAction(card, routingPrefix, mergeRequestId, mergeRequest);
        addApproveAction(card, routingPrefix, mergeRequestId, mergeRequest);
        addCommentAction(card, routingPrefix, mergeRequestId);

        return card.build();
    }

    private CardBody createBody(
            MergeRequestId mergeRequestId,
            MergeRequest mergeRequest
    ) {
        CardBody.Builder body = new CardBody.Builder();

        addInfo(body, mergeRequestId, mergeRequest);
        addChangeStats(body, mergeRequest);

        return body.build();
    }

    private void addInfo(
            CardBody.Builder body,
            MergeRequestId mergeRequestId,
            MergeRequest mergeRequest
    ) {
        body
                .setDescription(cardTextAccessor.getBody(mergeRequest.getDescription()))
                .addField(buildGeneralBodyField("repository", mergeRequestId.getNamespace(), mergeRequestId.getProjectName()))
                .addField(buildGeneralBodyField("requester", mergeRequest.getAuthor().getUsername()))
                .addField(buildGeneralBodyField("title", mergeRequest.getTitle()))
                .addField(buildGeneralBodyField("state", mergeRequest.getState()))
                .addField(buildGeneralBodyField("mergeable", mergeRequest.getMergeStatus()))
                .addField(
                        buildGeneralBodyField(
                                "createdAt",
                                DateTimeFormatter.ISO_INSTANT.format(mergeRequest.getCreatedAt().toInstant())
                        )
                )
                .addField(buildGeneralBodyField("comments", mergeRequest.getUserNotesCount()));
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

    private void addChangeStats(
            CardBody.Builder body,
            MergeRequest mergeRequest
    ) {
        body.addField(buildGeneralBodyField("changes", mergeRequest.getChangesCount()));
    }

    private void addCloseAction(
            Card.Builder card,
            String routingPrefix,
            MergeRequestId mergeRequestId,
            MergeRequest mergeRequest
    ) {
        if (mergeRequest.getState().isOpen()) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("close"))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("close"))
                            .setActionKey(CardActionKey.USER_INPUT)
                            .setUrl(getActionUrl(routingPrefix, mergeRequestId, "close"))
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
            MergeRequestId mergeRequestId,
            MergeRequest mergeRequest
    ) {
        if (mergeRequest.getState().isOpen() && mergeRequest.getMergeStatus().canBeMerged()) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("merge"))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("merge"))
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
            MergeRequest mergeRequest
    ) {
        if (mergeRequest.getState().isOpen() && isEnterpriseEdition) {
            card.addAction(
                    new CardAction.Builder()
                            .setLabel(cardTextAccessor.getActionLabel("approve"))
                            .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("approve"))
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
            MergeRequestId mergeRequestId
    ) {
        card.addAction(
                new CardAction.Builder()
                        .setLabel(cardTextAccessor.getActionLabel("comment"))
                        .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("comment"))
                        .setActionKey(CardActionKey.USER_INPUT)
                        .setUrl(getActionUrl(routingPrefix, mergeRequestId, "comment"))
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
    public Single<ResponseEntity<String>> comment(
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

    private Single<ResponseEntity<String>> postNote(
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

    private Single<ResponseEntity<String>> actionRequest(
            String auth,
            String baseUrl,
            MergeRequestId mergeRequestId,
            String action,
            HttpMethod method,
            Map<String, Object> body
    ) {
        HttpHeaders headers = makeHeaders(auth);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ListenableFuture<ResponseEntity<String>> response = rest.exchange(
                makeGitlabUri(baseUrl, mergeRequestId, action),
                method,
                request,
                String.class
        );

        return Async.toSingle(response);
    }

    @PostMapping(
            path = "/api/v1/{namespace}/{projectName}/{number}/close",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> close(
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

        Single<ResponseEntity<String>> noteResponse;

        if (StringUtils.isEmpty(reason)) {
            noteResponse = Single.just(ResponseEntity.ok("does not matter"));
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

    private Single<ResponseEntity<String>> closeMergeRequest(
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
    public Single<ResponseEntity<String>> merge(
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
    public Single<ResponseEntity<String>> approve(
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
