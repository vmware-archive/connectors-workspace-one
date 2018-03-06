/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.jira;

import com.google.common.collect.ImmutableMap;
import com.vmware.connectors.common.json.JsonDocument;
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestOperations;
import rx.Observable;
import rx.Single;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.*;

/**
 * Created by Rob Worsnop on 10/17/16.
 */
@RestController
public class JiraController {
    private final static Logger logger = LoggerFactory.getLogger(JiraController.class);
    private final static String JIRA_AUTH_HEADER = "x-jira-authorization";
    private final static String JIRA_BASE_URL_HEADER = "x-jira-base-url";
    private final static String ROUTING_PREFIX = "x-routing-prefix";

    private static final int COMMENTS_SIZE = 2;

    private final AsyncRestOperations rest;
    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public JiraController(AsyncRestOperations rest, CardTextAccessor cardTextAccessor) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
    }

    @PostMapping(path = "/cards/requests", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public Single<ResponseEntity<Cards>> getCards(
            @RequestHeader(name = JIRA_AUTH_HEADER) String jiraAuth,
            @RequestHeader(name = JIRA_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(name = ROUTING_PREFIX) String routingPrefix,
            @Valid @RequestBody CardRequest cardRequest) {

        Set<String> issueIds = cardRequest.getTokens("issue_id");
        if (CollectionUtils.isEmpty(issueIds)) {
            logger.debug("Empty jira issues for Jira server: {}", baseUrl);
            return Single.just(ResponseEntity.ok(new Cards()));
        }
        return Observable.from(issueIds)
                .flatMap(issueId -> getCardForIssue(jiraAuth, baseUrl, issueId, routingPrefix))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .map(ResponseEntity::ok)
                .toSingle();
    }

    @PostMapping(path = "/api/v1/issues/{issueKey}/comment", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public Single<ResponseEntity<Void>> addComment(
            @RequestHeader(name = JIRA_AUTH_HEADER) String jiraAuth,
            @RequestHeader(name = JIRA_BASE_URL_HEADER) String baseUrl,
            @PathVariable String issueKey, @RequestParam String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, jiraAuth);
        headers.set(CONTENT_TYPE, APPLICATION_JSON_VALUE);
        logger.debug("Adding jira comments for issue id : {} with Jira server: {}", issueKey, baseUrl);
        ListenableFuture<ResponseEntity<String>> future = rest.exchange(
                baseUrl + "/rest/api/2/issue/{issueKey}/comment", HttpMethod.POST,
                new HttpEntity<>(Collections.singletonMap("body", body), headers), String.class,
                issueKey);
        return Async.toSingle(future)
                .map(entity -> ResponseEntity.status(entity.getStatusCode()).build());
    }

    @PostMapping(path = "/api/v1/issues/{issueKey}/watchers")
    public Single<ResponseEntity<Void>> addWatcher(
            @RequestHeader(name = JIRA_AUTH_HEADER) String jiraAuth,
            @RequestHeader(name = JIRA_BASE_URL_HEADER) String baseUrl,
            @PathVariable String issueKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, jiraAuth);
        headers.setContentType(APPLICATION_JSON);
        logger.debug("Adding the user to watcher list for jira issue id : {} with jira server : {}", issueKey, baseUrl);

        ListenableFuture<ResponseEntity<JsonDocument>> selfFuture = rest.exchange(
                baseUrl + "/rest/api/2/myself", GET,
                new HttpEntity<>(headers), JsonDocument.class);

        return Async.toSingle(selfFuture)
                .flatMap(entity -> addUserToWatcher(entity.getBody(), headers, baseUrl, issueKey))
                .map(entity -> ResponseEntity.status(entity.getStatusCode()).build());
    }

    @GetMapping("/test-auth")
    public Single<ResponseEntity<Void>> verifyAuth(@RequestHeader(name = JIRA_AUTH_HEADER) String jiraAuth,
                                                   @RequestHeader(name = JIRA_BASE_URL_HEADER) String baseUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, jiraAuth);

        return Async.toSingle(rest.exchange(baseUrl + "/rest/api/2/myself", HttpMethod.HEAD, new HttpEntity<>(headers), Void.class))
                .map(ignored -> ResponseEntity.noContent().build());
    }

    private Single<ResponseEntity<Void>> addUserToWatcher(JsonDocument jiraUserDetails, HttpHeaders headers,
                                                          String baseUrl, String issueKey) {
        String user = jiraUserDetails.read("$.name");
        ListenableFuture<ResponseEntity<String>> addWatcherFuture = rest.exchange(
                baseUrl + "/rest/api/2/issue/{issueKey}/watchers", HttpMethod.POST,
                new HttpEntity<>(String.format("\"%s\"", user), headers), String.class,
                issueKey);
        return Async.toSingle(addWatcherFuture)
                .map(entity -> ResponseEntity.status(entity.getStatusCode()).build());

    }

    private Observable<Card> getCardForIssue(String jiraAuth, String baseUrl, String issueId, String routingPrefix) {
        return getIssue(jiraAuth, baseUrl, issueId).toObservable()
                // if an issue is not found, we'll just not bother creating a card
                .onErrorResumeNext(ObservableUtil::skip404)
                .map(entity -> transformIssueResponse(entity, baseUrl, issueId, routingPrefix));

    }

    private Single<ResponseEntity<JsonDocument>> getIssue(String jiraAuth, String baseUrl, String issueId) {
        logger.debug("Getting info for Jira id: {} with Jira server: {}", issueId, baseUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, jiraAuth);
        ListenableFuture<ResponseEntity<JsonDocument>> future = rest.exchange(
                baseUrl + "/rest/api/2/issue/{issueId}", GET, new HttpEntity<String>(headers), JsonDocument.class, issueId);
        return Async.toSingle(future);
    }

    private Card transformIssueResponse(ResponseEntity<JsonDocument> result, String baseUrl, String issueId, String routingPrefix) {
        JsonDocument jiraResponse = result.getBody();
        String issueKey = jiraResponse.read("$.key");
        String summary = jiraResponse.read("$.fields.summary");
        List<String> fixVersions = jiraResponse.read("$.fields.fixVersions[*].name");
        List<String> components = jiraResponse.read("$.fields.components[*].name");
        List<Map<String, Object>> allComments = jiraResponse.read("$.fields.comment.comments[*]['body', 'author']");
        Collections.reverse(allComments);

        CardAction.Builder commentActionBuilder = getCommentActionBuilder(jiraResponse, routingPrefix);
        CardAction.Builder watchActionBuilder = getWatchActionBuilder(jiraResponse, routingPrefix);
        CardAction.Builder openInActionBuilder = getOpenInActionBuilder(baseUrl, issueId);

        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .setDescription(summary)
                .addField(buildGeneralBodyField("project", jiraResponse.read("$.fields.project.name")))
                .addField(buildGeneralBodyField("components", String.join(",", components)))
                .addField(buildGeneralBodyField("priority", jiraResponse.read("$.fields.priority.name")))
                .addField(buildGeneralBodyField("status", jiraResponse.read("$.fields.status.name")))
                .addField(buildGeneralBodyField("resolution", jiraResponse.read("$.fields.resolution.name")))
                .addField(buildGeneralBodyField("assignee", jiraResponse.read("$.fields.assignee.displayName")))
                .addField(buildGeneralBodyField("fixVersions", String.join(",", fixVersions)));

        addCommentsField(cardBodyBuilder, allComments);

        return new Card.Builder()
                .setName("Jira")
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(cardTextAccessor.getHeader(summary), cardTextAccessor.getMessage("subtitle", issueKey))
                .setBody(cardBodyBuilder.build())
                .addAction(commentActionBuilder.build())
                .addAction(openInActionBuilder.build())
                .addAction(watchActionBuilder.build())
                .build();
    }

    private CardBodyField buildGeneralBodyField(String titleMessageKey, String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return new CardBodyField.Builder()
                .setTitle(cardTextAccessor.getMessage(titleMessageKey + ".title"))
                .setDescription(cardTextAccessor.getMessage(titleMessageKey + ".content", content))
                .setType(CardBodyFieldType.GENERAL)
                .build();
    }

    private void addCommentsField(CardBody.Builder cardBodyBuilder, List<Map<String, Object>> allComments) {
        CardBodyField.Builder cardFieldBuilder = new CardBodyField.Builder();

        if (!allComments.isEmpty()) {
            cardFieldBuilder.setTitle(cardTextAccessor.getMessage("comments.title"))
                    .setType(CardBodyFieldType.COMMENT);

            allComments.stream()
                    .limit(COMMENTS_SIZE)
                    .map(commentInfo -> ((Map<String, String>) commentInfo.get("author")).get("name") + " - " + commentInfo.get("body"))
                    .forEach(comment -> cardFieldBuilder.addContent(ImmutableMap.of("text", cardTextAccessor.getMessage("comments.content", comment))));
            cardBodyBuilder.addField(cardFieldBuilder.build());
        }
    }

    private CardAction.Builder getCommentActionBuilder(JsonDocument jiraResponse, String routingPrefix) {
        CardAction.Builder actionBuilder = new CardAction.Builder();
        CardActionInputField.Builder inputFieldBuilder = new CardActionInputField.Builder();
        String commentLink = "api/v1/issues/" + jiraResponse.read("$.id") + "/comment";
        inputFieldBuilder.setId("body")
                .setFormat("textarea")
                .setLabel(cardTextAccessor.getMessage("actions.comment.prompt.label"));
        actionBuilder.setLabel(cardTextAccessor.getActionLabel("actions.comment"))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("actions.comment"))
                .setActionKey("USER_INPUT")
                .setUrl(routingPrefix + commentLink)
                .setType(HttpMethod.POST)
                .addUserInputField(inputFieldBuilder.build());
        return actionBuilder;
    }

    private CardAction.Builder getWatchActionBuilder(JsonDocument jiraResponse, String routingPrefix) {
        CardAction.Builder actionBuilder = new CardAction.Builder();
        String watchLink = "api/v1/issues/" + jiraResponse.read("$.id") + "/watchers";
        actionBuilder.setLabel(cardTextAccessor.getActionLabel("actions.watch"))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("actions.watch"))
                .setActionKey(CardActionKey.DIRECT)
                .setUrl(routingPrefix + watchLink)
                .setType(HttpMethod.POST);
        return actionBuilder;
    }

    private CardAction.Builder getOpenInActionBuilder(String baseUrl, String issueId) {
        CardAction.Builder actionBuilder = new CardAction.Builder();
        String jiraIssueWebUrl = baseUrl + "/browse/" + issueId;
        actionBuilder.setLabel(cardTextAccessor.getActionLabel("actions.openIn"))
                .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel("actions.openIn"))
                .setActionKey(CardActionKey.OPEN_IN)
                .setUrl(jiraIssueWebUrl)
                .setType(GET);
        return actionBuilder;
    }
}
