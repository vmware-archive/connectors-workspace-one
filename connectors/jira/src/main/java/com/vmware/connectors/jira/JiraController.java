/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.jira;

import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardAction;
import com.vmware.connectors.common.payloads.response.CardActionInputField;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.CardBodyField;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.utils.Async;
import com.vmware.connectors.common.utils.CardTextAccessor;
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
import org.springframework.web.client.HttpClientErrorException;
import rx.Observable;
import rx.Single;

import javax.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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
                "{baseUrl}/rest/api/2/issue/{issueKey}/comment", HttpMethod.POST,
                new HttpEntity<>(Collections.singletonMap("body", body), headers), String.class,
                baseUrl, issueKey);
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
                "{baseUrl}/rest/api/2/myself", GET,
                new HttpEntity<>(headers), JsonDocument.class, baseUrl);

        return Async.toSingle(selfFuture)
                .flatMap(entity -> addUserToWatcher(entity.getBody(), headers, baseUrl, issueKey))
                .map(entity -> ResponseEntity.status(entity.getStatusCode()).build());
    }

    @GetMapping(path = "/test-auth")
    public Single<ResponseEntity<Void>> testAuth(@RequestHeader(name = JIRA_AUTH_HEADER) String jiraAuth,
                                                 @RequestHeader(name = JIRA_BASE_URL_HEADER) String baseUrl) {
        return getIssue(jiraAuth, baseUrl, "XYZ-999")
                .map(JiraController::stripBody)
                .onErrorResumeNext(JiraController::map404to200);
    }

    private Single<ResponseEntity<Void>> addUserToWatcher(JsonDocument jiraUserDetails, HttpHeaders headers,
                                                          String baseUrl, String issueKey) {
        String user = jiraUserDetails.read("$.name");
        ListenableFuture<ResponseEntity<String>> addWatcherFuture = rest.exchange(
                "{baseUrl}/rest/api/2/issue/{issueKey}/watchers", HttpMethod.POST,
                new HttpEntity<>(String.format("\"%s\"", user), headers), String.class,
                baseUrl, issueKey);
        return Async.toSingle(addWatcherFuture)
                .map(entity -> ResponseEntity.status(entity.getStatusCode()).build());

    }

    private Observable<Card> getCardForIssue(String jiraAuth, String baseUrl, String issueId, String routingPrefix) {
        return getIssue(jiraAuth, baseUrl, issueId).toObservable()
                // if an issue is not found, we'll just not bother creating a card
                .onErrorResumeNext(JiraController::skip404)
                .map(entity -> transformIssueResponse(entity, baseUrl, issueId, routingPrefix));

    }

    private Single<ResponseEntity<JsonDocument>> getIssue(String jiraAuth, String baseUrl, String issueId) {
        logger.debug("Getting info for Jira id: {} with Jira server: {}", issueId, baseUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, jiraAuth);
        ListenableFuture<ResponseEntity<JsonDocument>> future = rest.exchange(
                "{baseUrl}/rest/api/2/issue/{issueId}", GET, new HttpEntity<String>(headers), JsonDocument.class,
                baseUrl, issueId);
        return Async.toSingle(future);
    }

    private static Observable<ResponseEntity<JsonDocument>> skip404(Throwable throwable) {
        if (throwable instanceof HttpClientErrorException
                && HttpClientErrorException.class.cast(throwable).getStatusCode() == NOT_FOUND) {
            // It's OK to request non-existent Jira issues; we just won't create a card.
            return Observable.empty();
        } else {
            // If the problem is not 404, let the problem bubble up
            return Observable.error(throwable);
        }
    }

    private static ResponseEntity<Void> stripBody(ResponseEntity<JsonDocument> entity) {
        return ResponseEntity.status(entity.getStatusCode()).build();
    }

    private static Single<ResponseEntity<Void>> map404to200(Throwable throwable) {
        if (throwable instanceof HttpClientErrorException
                && HttpClientErrorException.class.cast(throwable).getStatusCode() == NOT_FOUND) {
            // It's OK to request non-existent Jira issues; we just won't create a card.
            return Single.just(ResponseEntity.ok().build());
        } else {
            // If the problem is not 404, let the problem bubble up
            return Single.error(throwable);
        }
    }

    private Card transformIssueResponse(ResponseEntity<JsonDocument> result, String baseUrl, String issueId, String routingPrefix) {
        JsonDocument jiraResponse = result.getBody();
        String issueKey = jiraResponse.read("$.key");
        String project = jiraResponse.read("$.fields.project.name");
        String summary = jiraResponse.read("$.fields.summary");
        String status = jiraResponse.read("$.fields.status.name");
        String assignee = jiraResponse.read("$.fields.assignee.displayName");
        String reporter = jiraResponse.read("$.fields.reporter.displayName");
        List<String> components = jiraResponse.read("$.fields.components[*].name");
        List<String> labels = jiraResponse.read("$.fields.labels");
        String description = jiraResponse.read("$.fields.description");
        List<String> allComments = jiraResponse.read("$.fields.comment.comments[*].body");
        Collections.reverse(allComments);
        Map<String, String> lastComment = new HashMap<>();
        Map<String, String> secondToLastComment = new HashMap<>();

        final String GENERAL_CARD_TYPE = "GENERAL";
        final String COMMENT_CARD_TYPE = "COMMENT";

        CardAction.Builder commentActionBuilder = getCommentActionBuilder(jiraResponse, routingPrefix);
        CardAction.Builder watchActionBuilder = getWatchActionBuilder(jiraResponse, routingPrefix);
        CardAction.Builder openInActionBuilder = getOpenInActionBuilder(baseUrl, issueId);

        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .setDescription(cardTextAccessor.getBody(summary));

        CardBodyField.Builder cardFieldBuilder = new CardBodyField.Builder()
                .setTitle(cardTextAccessor.getMessage("project"))
                .setDescription(project)
                .setType(GENERAL_CARD_TYPE);
        cardBodyBuilder.addField(cardFieldBuilder.build());

        cardFieldBuilder.setTitle(cardTextAccessor.getMessage("summary"))
                .setDescription(summary)
                .setType(GENERAL_CARD_TYPE);
        cardBodyBuilder.addField(cardFieldBuilder.build());

        cardFieldBuilder.setTitle(cardTextAccessor.getMessage("status"))
                .setDescription(status)
                .setType(GENERAL_CARD_TYPE);
        cardBodyBuilder.addField(cardFieldBuilder.build());

        cardFieldBuilder.setTitle(cardTextAccessor.getMessage("assignee"))
                .setDescription(assignee)
                .setType(GENERAL_CARD_TYPE);
        cardBodyBuilder.addField(cardFieldBuilder.build());

        cardFieldBuilder.setTitle(cardTextAccessor.getMessage("reporter"))
                .setDescription(reporter)
                .setType(GENERAL_CARD_TYPE);
        cardBodyBuilder.addField(cardFieldBuilder.build());

        cardFieldBuilder.setTitle(cardTextAccessor.getMessage("components"))
                .setDescription(String.join(",", components))
                .setType(GENERAL_CARD_TYPE);
        cardBodyBuilder.addField(cardFieldBuilder.build());

        cardFieldBuilder.setTitle(cardTextAccessor.getMessage("labels"))
                .setDescription(String.join(",", labels))
                .setType(GENERAL_CARD_TYPE);
        cardBodyBuilder.addField(cardFieldBuilder.build());

        cardFieldBuilder.setTitle(cardTextAccessor.getMessage("description"))
                .setDescription(description)
                .setType(GENERAL_CARD_TYPE);
        cardBodyBuilder.addField(cardFieldBuilder.build());

        if (!allComments.isEmpty()) {
            cardFieldBuilder.setTitle(cardTextAccessor.getMessage("comments"))
                    .setType(COMMENT_CARD_TYPE);

            lastComment.put("text", allComments.get(0));
            cardFieldBuilder.addContent(lastComment);
            if (allComments.size() > 1) {
                secondToLastComment.put("text", allComments.get(1));
                cardFieldBuilder.addContent(secondToLastComment);
            }
            cardBodyBuilder.addField(cardFieldBuilder.build());
        }

        Card.Builder cardBuilder = new Card.Builder()
                .setName("Jira")
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(cardTextAccessor.getHeader(issueKey), null)
                .setBody(cardBodyBuilder.build())
                .addAction(commentActionBuilder.build())
                .addAction(openInActionBuilder.build())
                .addAction(watchActionBuilder.build());
        return cardBuilder.build();
    }

    private CardAction.Builder getCommentActionBuilder(JsonDocument jiraResponse, String routingPrefix) {
        CardAction.Builder actionBuilder = new CardAction.Builder();
        CardActionInputField.Builder inputFieldBuilder = new CardActionInputField.Builder();
        String commentLink = "api/v1/issues/" + jiraResponse.read("$.id") + "/comment";
        inputFieldBuilder.setId("body")
                .setFormat("textarea")
                .setLabel("Comment");
        actionBuilder.setLabel(cardTextAccessor.getActionLabel("comment"))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("comment"))
                .setActionKey("USER_INPUT")
                .setUrl(routingPrefix + commentLink)
                .setType(HttpMethod.POST)
                .addUserInputField(inputFieldBuilder.build());
        return actionBuilder;
    }

    private CardAction.Builder getWatchActionBuilder(JsonDocument jiraResponse, String routingPrefix) {
        CardAction.Builder actionBuilder = new CardAction.Builder();
        String watchLink = "api/v1/issues/" + jiraResponse.read("$.id") + "/watchers";
        actionBuilder.setLabel(cardTextAccessor.getActionLabel("watch"))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("watch"))
                .setActionKey("DIRECT")
                .setUrl(routingPrefix + watchLink)
                .setType(HttpMethod.POST);
        return actionBuilder;
    }

    private CardAction.Builder getOpenInActionBuilder(String baseUrl, String issueId) {
        CardAction.Builder actionBuilder = new CardAction.Builder();
        String jiraIssueWebUrl = baseUrl + "/browse/" + issueId;
        actionBuilder.setLabel(cardTextAccessor.getActionLabel("openin"))
                .setActionKey("OPEN_IN")
                .setUrl(jiraIssueWebUrl)
                .setType(GET);
        return actionBuilder;
    }
}
