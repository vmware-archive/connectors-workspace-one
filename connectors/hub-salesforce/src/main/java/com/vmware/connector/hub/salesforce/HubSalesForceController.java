package com.vmware.connector.hub.salesforce;

import com.google.common.collect.ImmutableList;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.vmware.connectors.common.utils.CommonUtils.APPROVAL_ACTIONS;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@RestController
public class HubSalesForceController {

    private static final Logger logger = LoggerFactory.getLogger(HubSalesForceController.class);

    private static final String AUTH_HEADER = "X-Connector-Authorization";
    private static final String BASE_URL_HEADER = "X-Connector-Base-Url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";

    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;

    private final static String REASON = "reason";
    private final static String USER_ID = "userId";

    private final static String WORK_ITEMS_QUERY = "SELECT Id,TargetObjectid, Status,(select id,actor.name, actor.id, actor.email, actor.username from Workitems Where actor.email = '%s'),(SELECT Id, StepStatus, Comments,Actor.Name, Actor.Id, actor.email, actor.username FROM Steps) FROM ProcessInstance Where Status = 'Pending'";
    private final static String OPPORTUNITY_QUERY = "SELECT Id, Name, FORMAT(ExpectedRevenue), Account.Owner.Name FROM opportunity WHERE Id IN ('%s')";

    private final String sfSoqlQueryPath;
    private final String workflowPath;

    @Autowired
    public HubSalesForceController(final WebClient rest,
                                   final CardTextAccessor cardTextAccessor,
                                   @Value("${sf.soqlQueryPath}") final String sfSoqlQueryPath,
                                   @Value("${sf.workflowPath}") final String workflowPath) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.sfSoqlQueryPath = sfSoqlQueryPath;
        this.workflowPath = workflowPath;
    }

    @PostMapping(
            path = "/cards/requests",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Cards>> getCards(
            @RequestHeader(AUTHORIZATION) final String auth,
            @RequestHeader(AUTH_HEADER) final String connectorAuth,
            @RequestHeader(BASE_URL_HEADER) final String baseUrl,
            @RequestHeader(ROUTING_PREFIX) final String routingPrefix,
            final HttpServletRequest request,
            final Locale locale
    ) throws IOException {
        logger.trace("getCards called with baseUrl: {} and routingPrefix: {}", baseUrl, routingPrefix);

        final String userEmail = AuthUtil.extractUserEmail(auth);
        if (StringUtils.isBlank(userEmail)) {
            logger.error("User email (eml) is empty in jwt access token.");
            return Mono.just(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        }

        return retrieveWorkItems(connectorAuth, baseUrl, "ssathiamoort@vmware.com")
                .flatMapMany(result -> processWorkItemResult(result, baseUrl, connectorAuth, locale, routingPrefix, request))
                .collectList()
                .map(this::toCards)
                .map(ResponseEntity::ok);
    }

    @PostMapping(
            path = "/api/expense/approve/{userId}",
            consumes = APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<Void> approveWorkFlow(
            @RequestHeader(AUTH_HEADER) final String connectorAuth,
            @RequestHeader(BASE_URL_HEADER) final String baseUrl,
            @RequestParam(REASON) final String comment,
            @PathVariable(USER_ID) final String userId
    ) {
        final ApprovalRequest request = new ApprovalRequest();
        request.setActionType(ApprovalRequestType.APPROVE.getType());
        request.setComments(comment);
        request.setContextId(userId);

        final ApprovalRequests requests = new ApprovalRequests();
        requests.setRequests(ImmutableList.of(request));

        return rest.post()
                .uri(fromHttpUrl(baseUrl).path(workflowPath).build().toUri())
                .header(AUTHORIZATION, connectorAuth)
                .contentType(APPLICATION_JSON)
                .syncBody(requests)
                .retrieve()
                .bodyToMono(Void.class);
    }

    @PostMapping(
            path = "/api/expense/reject/{userId}",
            consumes = APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<Void> rejectWorkFlow(
            @RequestHeader(AUTH_HEADER) final String connectorAuth,
            @RequestHeader(BASE_URL_HEADER) final String baseUrl,
            @RequestParam(REASON) final String reason,
            @PathVariable(USER_ID) final String userId
    ) {
        final ApprovalRequest request = new ApprovalRequest();
        request.setContextId(userId);
        request.setComments(reason);
        request.setActionType(ApprovalRequestType.REJECT.getType());

        final ApprovalRequests requests = new ApprovalRequests();
        requests.setRequests(ImmutableList.of(request));

        return rest.post()
                .uri(fromHttpUrl(baseUrl).path(workflowPath).build().toUri())
                .header(AUTHORIZATION, connectorAuth)
                .contentType(APPLICATION_JSON)
                .syncBody(requests)
                .retrieve()
                .bodyToMono(Void.class);
    }


    private Cards toCards(final List<Card> cardList) {
        final Cards cards = new Cards();
        cards.getCards().addAll(cardList);
        return cards;
    }

    private Flux<Card> processWorkItemResult(final JsonDocument workItemResponse,
                                             final String baseUrl,
                                             final String connectorAuth,
                                             final Locale locale,
                                             final String routingPrefix,
                                             final HttpServletRequest request) {
        final List<String> opportunityIds = workItemResponse.read("$.records[*].TargetObjectId");

        return retrieveOpportunities(baseUrl, opportunityIds, connectorAuth)
                .flatMapMany(opportunityResponse -> buildCards(workItemResponse, opportunityResponse, locale, routingPrefix, request));
    }

    private Mono<JsonDocument> retrieveOpportunities(String baseUrl, List<String> opportunityIds, String connectorAuth) {
        final String idsFormat = opportunityIds.stream()
                .collect(Collectors.joining(","));

        String soql = String.format(OPPORTUNITY_QUERY, idsFormat);
        return rest.get()
                .uri(makeSoqlQueryUri(baseUrl, soql))
                .header(AUTHORIZATION, connectorAuth)
                .retrieve()
                .bodyToMono(JsonDocument.class);
    }

    private Flux<Card> buildCards(final JsonDocument workItemResponse,
                                  final JsonDocument opportunityResponse,
                                  final Locale locale,
                                  final String routingPrefix,
                                  final HttpServletRequest request) {
        final int totalSize = workItemResponse.read("$.totalSize");
        final List<Card> cardList = new ArrayList<>();

        for (int i = 0; i < totalSize; i++) {
            final String opportunityName = opportunityResponse.read(String.format("$.records[%s].Name", i));
            final String opportunityOwnerName = opportunityResponse.read(String.format("$.records[%s].Account.Owner.Name", i));
            final String expectedRevenue = opportunityResponse.read(String.format("$.records[%s].ExpectedRevenue", i));

            final String userId = workItemResponse.read(String.format("$.records[%s].Workitems.records[0].Id", i));

            final CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                    .addField(buildCardBodyField("customer.name", opportunityName, locale))
                    .addField(buildCardBodyField("opportunity.owner", opportunityOwnerName, locale))
                    .addField(buildCardBodyField("revenue.opportunity", expectedRevenue, locale));

            final Card.Builder card = new Card.Builder()
                    .setName("Salesforce for WS1 Hub")
                    .setTemplate(routingPrefix + "templates/generic.hbs")
                    .setHeader(this.cardTextAccessor.getMessage("ws1.sf.card.header", locale))
                    .setBody(cardBodyBuilder.build())
                    .addAction(buildApproveAction(routingPrefix, locale, userId))
                    .addAction(buildRejectAction(routingPrefix, locale, userId));

            CommonUtils.buildConnectorImageUrl(card, request);

            cardList.add(card.build());
        }

        return Flux.fromIterable(cardList);
    }

    private CardAction buildApproveAction(final String routingPrefix,
                                          final Locale locale,
                                          final String userId) {
        final String approveUrl = "/api/expense/approve/" + userId;

        return new CardAction.Builder()
                .setLabel(this.cardTextAccessor.getActionLabel("ws1.sf.approve", locale))
                .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel("ws1.sf.approve", locale))
                .setActionKey(CardActionKey.USER_INPUT)
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + approveUrl)
                .setPrimary(true)
                .setMutuallyExclusiveSetId(APPROVAL_ACTIONS)
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setId("reason")
                                .setMinLength(1)
                                .setFormat("textarea")
                                .setLabel(cardTextAccessor.getMessage("ws1.sf.approve.reason.label", locale))
                                .build()
                ).build();
    }

    private CardAction buildRejectAction(final String routingPrefix,
                                         final Locale locale,
                                         final String userId) {
        final String rejectUrl = "/api/expense/reject/" + userId;

        return new CardAction.Builder()
                .setLabel(this.cardTextAccessor.getActionLabel("ws1.sf.reject", locale))
                .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel("ws1.sf.reject", locale))
                .setActionKey(CardActionKey.USER_INPUT)
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + rejectUrl)
                .setPrimary(true)
                .setMutuallyExclusiveSetId(APPROVAL_ACTIONS)
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setId("reason")
                                .setMinLength(1)
                                .setFormat("textarea")
                                .setLabel(cardTextAccessor.getMessage("ws1.sf.reject.reason.label", locale))
                                .build()
                ).build();
    }

    private CardBodyField buildCardBodyField(final String title,
                                             final String description,
                                             final Locale locale) {
        if (StringUtils.isBlank(description)) {
            return null;
        }

        return new CardBodyField.Builder()
                .setTitle(this.cardTextAccessor.getMessage("ws1.sf." + title, locale))
                .setDescription(description)
                .setType(CardBodyFieldType.GENERAL)
                .build();
    }

    private Mono<JsonDocument> retrieveWorkItems(final String connectorAuth,
                                                 final String baseUrl,
                                                 final String userEmail) {
        final String sql = String.format(WORK_ITEMS_QUERY, userEmail);

        return rest.get()
                .uri(makeSoqlQueryUri(baseUrl, sql))
                .header(AUTHORIZATION, connectorAuth)
                .retrieve()
                .bodyToMono(JsonDocument.class);
    }

    private URI makeSoqlQueryUri(
            final String baseUrl,
            final String soql
    ) {

        return fromHttpUrl(baseUrl)
                .path(sfSoqlQueryPath)
                .queryParam("q", soql)
                .build()
                .toUri();
    }
}
