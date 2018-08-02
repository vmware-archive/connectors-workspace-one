/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.jira;

import com.google.common.collect.ImmutableMap;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.CommonUtils;
import com.vmware.connectors.common.utils.Reactive;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.CREATED;
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

    private final String metadata;
    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public JiraController(WebClient rest, CardTextAccessor cardTextAccessor,
                          @Value("classpath:static/discovery/metadata.json") Resource metadataJsonResource) throws IOException {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.metadata = IOUtils.toString(metadataJsonResource.getInputStream(), Charset.defaultCharset());
    }

    @GetMapping(path = "/discovery/metadata.json")
    public ResponseEntity<String> getMetadata(HttpServletRequest request) {
        return ResponseEntity.ok(
                this.metadata.replace("${CONNECTOR_HOST}", CommonUtils.buildConnectorUrl(request, null)));
    }

    @PostMapping(path = "/cards/requests", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public Mono<Cards> getCards(
            @RequestHeader(name = JIRA_AUTH_HEADER) String jiraAuth,
            @RequestHeader(name = JIRA_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(name = ROUTING_PREFIX) String routingPrefix,
            Locale locale,
            @Valid @RequestBody CardRequest cardRequest,
            final HttpServletRequest request) {

        Set<String> issueIds = cardRequest.getTokens("issue_id");

        return Flux.fromIterable(issueIds)
                .flatMap(issueId -> getCardForIssue(jiraAuth, baseUrl, issueId,
                        routingPrefix, locale, request))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .defaultIfEmpty(new Cards())
                .subscriberContext(Reactive.setupContext());
    }

    @PostMapping(path = "/api/v1/issues/{issueKey}/comment", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Object>> addComment(
            @RequestHeader(name = JIRA_AUTH_HEADER) String jiraAuth,
            @RequestHeader(name = JIRA_BASE_URL_HEADER) String baseUrl,
            @PathVariable String issueKey, @RequestParam String body) {
        logger.debug("Adding jira comments for issue id : {} with Jira server: {}", issueKey, baseUrl);
        return rest.post()
                .uri(baseUrl + "/rest/api/2/issue/{issueKey}/comment", issueKey)
                .header(AUTHORIZATION, jiraAuth)
                .contentType(APPLICATION_JSON)
                .syncBody(Collections.singletonMap("body", body))
                .retrieve()
                .bodyToMono(String.class)
                .then(Mono.just(ResponseEntity.status(CREATED).build()))
                .subscriberContext(Reactive.setupContext());
    }

    @PostMapping(path = "/api/v1/issues/{issueKey}/watchers")
    public Mono<ResponseEntity<Void>> addWatcher(
            @RequestHeader(name = JIRA_AUTH_HEADER) String jiraAuth,
            @RequestHeader(name = JIRA_BASE_URL_HEADER) String baseUrl,
            @PathVariable String issueKey) {
        logger.debug("Adding the user to watcher list for jira issue id : {} with jira server : {}", issueKey, baseUrl);
        return rest.get()
                .uri(baseUrl + "/rest/api/2/myself")
                .header(AUTHORIZATION, jiraAuth)
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .flatMap(body -> addUserToWatcher(body, jiraAuth, baseUrl, issueKey))
                .map(status -> ResponseEntity.status(status).<Void>build())
                .subscriberContext(Reactive.setupContext());
    }

    @GetMapping("/test-auth")
    public Mono<ResponseEntity<Void>> verifyAuth(@RequestHeader(name = JIRA_AUTH_HEADER) String jiraAuth,
                                                 @RequestHeader(name = JIRA_BASE_URL_HEADER) String baseUrl) {
        return rest.head()
                .uri(baseUrl + "/rest/api/2/myself")
                .header(AUTHORIZATION, jiraAuth)
                .retrieve()
                .bodyToMono(String.class)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .subscriberContext(Reactive.setupContext());
    }

    private Mono<HttpStatus> addUserToWatcher(JsonDocument jiraUserDetails, String jiraAuth,
                                              String baseUrl, String issueKey) {
        String user = jiraUserDetails.read("$.name");
        return rest.post()
                .uri(baseUrl + "/rest/api/2/issue/{issueKey}/watchers", issueKey)
                .header(AUTHORIZATION, jiraAuth)
                .contentType(APPLICATION_JSON)
                .syncBody(String.format("\"%s\"", user))
                .exchange()
                .map(ClientResponse::statusCode);
    }

    private Mono<Card> getCardForIssue(String jiraAuth,
                                       String baseUrl,
                                       String issueId,
                                       String routingPrefix,
                                       Locale locale,
                                       HttpServletRequest request) {
        return getIssue(jiraAuth, baseUrl, issueId)
                // if an issue is not found, we'll just not bother creating a card
                .onErrorResume(Reactive::skipOnNotFound)
                .flatMap(Reactive.wrapMapper(jiraResponse -> transformIssueResponse(jiraResponse,
                        baseUrl,
                        issueId,
                        routingPrefix,
                        locale,
                        request)))
                .doOnEach(Reactive.wrapForItem(card -> logger.debug("Created card. {} -> {}", issueId, card.getHeader().getTitle())));

    }

    private Mono<JsonDocument> getIssue(String jiraAuth, String baseUrl, String issueId) {
        logger.debug("Getting info for Jira id: {} with Jira server: {}", issueId, baseUrl);
        return rest.get()
                .uri(baseUrl + "/rest/api/2/issue/{issueId}", issueId)
                .header(AUTHORIZATION, jiraAuth)
                .retrieve()
                .bodyToMono(JsonDocument.class);
    }

    private Card transformIssueResponse(JsonDocument jiraResponse,
                                        String baseUrl,
                                        String issueId,
                                        String routingPrefix,
                                        Locale locale,
                                        HttpServletRequest request) {
        String issueKey = jiraResponse.read("$.key");
        String summary = jiraResponse.read("$.fields.summary");
        List<String> fixVersions = jiraResponse.read("$.fields.fixVersions[*].name");
        List<String> components = jiraResponse.read("$.fields.components[*].name");
        List<Map<String, Object>> allComments = jiraResponse.read("$.fields.comment.comments[*]['body', 'author']");
        Collections.reverse(allComments);

        CardAction.Builder commentActionBuilder = getCommentActionBuilder(jiraResponse, routingPrefix, locale);
        CardAction.Builder watchActionBuilder = getWatchActionBuilder(jiraResponse, routingPrefix, locale);
        CardAction.Builder openInActionBuilder = getOpenInActionBuilder(baseUrl, issueId, locale);

        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .addField(buildGeneralBodyField("project", jiraResponse.read("$.fields.project.name"), locale))
                .addField(buildGeneralBodyField("components", String.join(",", components), locale))
                .addField(buildGeneralBodyField("priority", jiraResponse.read("$.fields.priority.name"), locale))
                .addField(buildGeneralBodyField("status", jiraResponse.read("$.fields.status.name"), locale))
                .addField(buildGeneralBodyField("resolution", jiraResponse.read("$.fields.resolution.name"), locale))
                .addField(buildGeneralBodyField("assignee", jiraResponse.read("$.fields.assignee.displayName"), locale))
                .addField(buildGeneralBodyField("fixVersions", String.join(",", fixVersions), locale));

        addCommentsField(cardBodyBuilder, allComments, locale);

        final Card.Builder card = new Card.Builder()
                .setName("Jira")
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(cardTextAccessor.getHeader(locale, summary), cardTextAccessor.getMessage("subtitle", locale, issueKey))
                .setBody(cardBodyBuilder.build())
                .addAction(commentActionBuilder.build())
                .addAction(openInActionBuilder.build())
                .addAction(watchActionBuilder.build());

        // Set image url.
        CommonUtils.buildConnectorImageUrl(card, request);

        return card.build();
    }

    private CardBodyField buildGeneralBodyField(String titleMessageKey, String content, Locale locale) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return new CardBodyField.Builder()
                .setTitle(cardTextAccessor.getMessage(titleMessageKey + ".title", locale))
                .setDescription(cardTextAccessor.getMessage(titleMessageKey + ".content", locale, content))
                .setType(CardBodyFieldType.GENERAL)
                .build();
    }

    private void addCommentsField(CardBody.Builder cardBodyBuilder, List<Map<String, Object>> allComments, Locale locale) {
        CardBodyField.Builder cardFieldBuilder = new CardBodyField.Builder();

        if (!allComments.isEmpty()) {
            cardFieldBuilder.setTitle(cardTextAccessor.getMessage("comments.title", locale))
                    .setType(CardBodyFieldType.COMMENT);

            allComments.stream()
                    .limit(COMMENTS_SIZE)
                    .map(commentInfo -> ((Map<String, String>) commentInfo.get("author")).get("name") + " - " + commentInfo.get("body"))
                    .forEach(comment -> cardFieldBuilder.addContent(ImmutableMap.of("text",
                            cardTextAccessor.getMessage("comments.content", locale, comment))));
            cardBodyBuilder.addField(cardFieldBuilder.build());
        }
    }

    private CardAction.Builder getCommentActionBuilder(JsonDocument jiraResponse, String routingPrefix, Locale locale) {
        CardAction.Builder actionBuilder = new CardAction.Builder();
        CardActionInputField.Builder inputFieldBuilder = new CardActionInputField.Builder();
        String commentLink = "api/v1/issues/" + jiraResponse.read("$.id") + "/comment";
        inputFieldBuilder.setId("body")
                .setFormat("textarea")
                .setLabel(cardTextAccessor.getMessage("actions.comment.prompt.label", locale));
        actionBuilder.setLabel(cardTextAccessor.getActionLabel("actions.comment", locale))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("actions.comment", locale))
                .setActionKey("USER_INPUT")
                .setUrl(routingPrefix + commentLink)
                .setType(HttpMethod.POST)
                .setAllowRepeated(true)
                .addUserInputField(inputFieldBuilder.build());
        return actionBuilder;
    }

    private CardAction.Builder getWatchActionBuilder(JsonDocument jiraResponse,
                                                     String routingPrefix, Locale locale) {
        CardAction.Builder actionBuilder = new CardAction.Builder();
        String watchLink = "api/v1/issues/" + jiraResponse.read("$.id") + "/watchers";
        actionBuilder.setLabel(cardTextAccessor.getActionLabel("actions.watch", locale))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("actions.watch", locale))
                .setActionKey(CardActionKey.DIRECT)
                .setUrl(routingPrefix + watchLink)
                .setType(HttpMethod.POST);
        return actionBuilder;
    }

    private CardAction.Builder getOpenInActionBuilder(String baseUrl, String issueId, Locale locale) {
        CardAction.Builder actionBuilder = new CardAction.Builder();
        String jiraIssueWebUrl = baseUrl + "/browse/" + issueId;
        actionBuilder.setLabel(cardTextAccessor.getActionLabel("actions.openIn", locale))
                .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel("actions.openIn", locale))
                .setActionKey(CardActionKey.OPEN_IN)
                .setAllowRepeated(true)
                .setUrl(jiraIssueWebUrl)
                .setType(GET);
        return actionBuilder;
    }
}
