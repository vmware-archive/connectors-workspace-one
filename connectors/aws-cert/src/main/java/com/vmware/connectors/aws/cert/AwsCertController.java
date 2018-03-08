/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.aws.cert;

import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.Async;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.ObservableUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import rx.Observable;
import rx.Single;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class AwsCertController {

    private static final String APPROVAL_URL_PARAM = "hero_aws_cert_approval_url";

    private static final Logger logger = LoggerFactory.getLogger(AwsCertController.class);

    private static final String ROUTING_PREFIX = "x-routing-prefix";

    private static final String APPROVE_PATH = "/api/v1/approve";

    private final String certificateApprovalHost;
    private final String certificateApprovalPath;
    private final AsyncRestOperations rest;
    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public AwsCertController(
            @Value("${aws.certificate.connector.approval.host}") String certificateApprovalHost,
            @Value("${aws.certificate.connector.approval.path}") String certificateApprovalPath,
            AsyncRestOperations rest,
            CardTextAccessor cardTextAccessor
    ) {
        this.certificateApprovalHost = certificateApprovalHost.toLowerCase(Locale.US);
        this.certificateApprovalPath = certificateApprovalPath;
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
    }

    @PostMapping(
            path = "/cards/requests",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Single<ResponseEntity<Cards>> getCards(
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            @Valid @RequestBody CardRequest request
    ) {
        logger.trace("getCards called, routingPrefix={}, request={}", routingPrefix, request);

        List<String> approvalUrls = new ArrayList<>(request.getTokens("approval_urls"));

        if (CollectionUtils.isEmpty(approvalUrls)) {
            return Single.just(ResponseEntity.ok(new Cards()));
        }

        approvalUrls = validateUrls(approvalUrls);

        Collections.sort(approvalUrls);

        return getAllCardsInfo(new LinkedHashSet<>(approvalUrls))
                .filter(pair -> pair.getRight().getStatusCode().is2xxSuccessful())
                .map(this::parseCardInfoOutOfResponse)
                .reduce(
                        new Cards(),
                        (cards, info) -> appendCard(cards, info, routingPrefix)
                )
                .toSingle()
                .map(ResponseEntity::ok);
    }

    private List<String> validateUrls(List<String> approvalUrls) {
        return approvalUrls.stream()
                .filter(this::validateUrl)
                .collect(Collectors.toList());
    }

    private boolean validateUrl(String approvalUrl) {
        boolean isValid;
        try {
            UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(approvalUrl).build();
            isValid = verifyHost(uriComponents) && verifyPath(uriComponents);
        } catch (IllegalArgumentException e) {
            isValid = false;
        }
        if (!isValid) {
            logger.warn("Approval url was not valid: {}", approvalUrl);
        }
        return isValid;
    }

    private boolean verifyHost(UriComponents uriComponents) {
        String host = uriComponents.getHost().toLowerCase(Locale.US);
        return host.equals(certificateApprovalHost)
                || host.endsWith(certificateApprovalHost)
                && host.charAt(host.lastIndexOf(certificateApprovalHost) - 1) == '.';
    }

    private boolean verifyPath(UriComponents uriComponents) {
        return uriComponents.getPath().equals(certificateApprovalPath);
    }

    private Observable<Pair<String, ResponseEntity<String>>> getAllCardsInfo(Set<String> approvalUrls) {
        return Observable
                .from(approvalUrls)
                .flatMap(this::callForCardInfo);
    }

    private Observable<Pair<String, ResponseEntity<String>>> callForCardInfo(String approvalUrl) {
        logger.trace("callForCardInfo called: approvalUrl={}", approvalUrl);

        ListenableFuture<ResponseEntity<String>> response = rest.exchange(
                UriComponentsBuilder
                        .fromHttpUrl(approvalUrl)
                        .build()
                        .toUri(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class
        );

        return Async.toSingle(response)
                .toObservable()
                // Don't let a bad AWS token skip the rest
                .onErrorResumeNext(ObservableUtil::skip400) // Expired requests will return 400 bad request
                .onErrorResumeNext(ObservableUtil::skip404) // Non-existent contexts will return 404 not found
                .map(responseEntity -> Pair.of(approvalUrl, responseEntity));
    }

    private AwsCertCardInfo parseCardInfoOutOfResponse(Pair<String, ResponseEntity<String>> pair) {
        String approvalUrl = pair.getLeft();
        logger.trace("parseCardInfoOutOfResponse called: approvalUrl={}", approvalUrl);

        String html = pair.getRight().getBody();

        Document doc = Jsoup.parse(html);
        Elements elements = doc.body().children();
        Elements rows = elements.select("table > tbody > tr");

        AwsCertCardInfo info = collectInfo(rows);

        Elements formElements = elements.select("form");
        Map<String, String> formParams = collectFormParams(formElements);

        // Supplement the form params with the approvalUrl so the client will tell the approve action who to POST to.
        formParams.put(APPROVAL_URL_PARAM, approvalUrl);

        info.setFormParams(formParams);

        return info;
    }

    private AwsCertCardInfo collectInfo(Elements rows) {
        AwsCertCardInfo info = new AwsCertCardInfo();

        rows.forEach(row -> {
            String label = row.child(0).text().toLowerCase(Locale.US);
            String value = row.child(1).text();

            fuzzySetDomainName(info, label, value);
            fuzzySetAccountId(info, label, value);
            fuzzySetRegionName(info, label, value);
            fuzzySetCertIdentifier(info, label, value);
        });

        return info;
    }

    private void fuzzySetDomainName(AwsCertCardInfo info, String label, String value) {
        if (label.contains("domain")) {
            info.setDomain(value);
        }
    }

    private void fuzzySetAccountId(AwsCertCardInfo info, String label, String value) {
        if (label.contains("account")) {
            info.setAccountId(value);
        }
    }

    private void fuzzySetRegionName(AwsCertCardInfo info, String label, String value) {
        if (label.contains("region")) {
            info.setRegionName(value);
        }
    }

    private void fuzzySetCertIdentifier(AwsCertCardInfo info, String label, String value) {
        if (label.contains("certificate")) {
            info.setCertIdentifier(value);
        }
    }

    private Map<String, String> collectFormParams(Elements formElements) {
        Map<String, String> formParams = new HashMap<>();
        List<FormElement> forms = formElements.forms();

        forms.forEach(
                form ->
                        form.formData().forEach(
                                kvp -> formParams.put(kvp.key(), kvp.value())
                        )
        );

        return formParams;
    }

    private Cards appendCard(Cards cards, AwsCertCardInfo info, String routingPrefix) {
        logger.trace("appendCard called: info={}, routingPrefix={}", info, routingPrefix);

        cards.getCards()
                .add(makeCard(info, routingPrefix));

        return cards;
    }

    private Card makeCard(
            AwsCertCardInfo info,
            String routingPrefix
    ) {
        logger.trace("makeCard called: info={}, routingPrefix={}", info, routingPrefix);

        CardAction.Builder approveAction = new CardAction.Builder()
                .setLabel(cardTextAccessor.getActionLabel("approve"))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("approve"))
                .setActionKey(CardActionKey.DIRECT)
                .setPrimary(true)
                .setRemoveCardOnCompletion(true)
                .setUrl(routingPrefix + APPROVE_PATH.substring(1))
                .setType(HttpMethod.POST);

        CardAction.Builder dismissAction = CardAction.Builder
                .dismissAction()
                .setLabel(cardTextAccessor.getActionLabel("dismiss"));

        info.getFormParams().forEach(approveAction::addRequestParam);

        return new Card.Builder()
                .setName("AwsCert") // TODO - remove this in APF-536
                .setCreationDate(OffsetDateTime.now())
                /*
                 * AWS Cert requests expire in 3 days, so anything beyond 3
                 * days of requesting the card is definitely expired.
                 *
                 * Since we don't know when the the cert was actually
                 * requested and the AWS approval page doesn't provide any
                 * information for when the cert was requested, we still might
                 * be sending cards for expired cert requests.
                 */
                .setExpirationDate(OffsetDateTime.now().plusDays(3))
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader("[AWS-Cert] " + cardTextAccessor.getHeader(), cardTextAccessor.getMessage("subtitle", info.getDomain()))
                .setBody(
                        new CardBody.Builder()
                                .setDescription(
                                        cardTextAccessor.getBody(
                                                info.getDomain(),
                                                info.getAccountId(),
                                                info.getRegionName(),
                                                info.getCertIdentifier()
                                        )
                                )
                                .addField(
                                        new CardBodyField.Builder()
                                                .setTitle(cardTextAccessor.getMessage("domain.title"))
                                                .setType(CardBodyFieldType.GENERAL)
                                                .setDescription(cardTextAccessor.getMessage("domain.description", info.getDomain()))
                                                .build()
                                )
                                .addField(
                                        new CardBodyField.Builder()
                                                .setTitle(cardTextAccessor.getMessage("accountId.title"))
                                                .setType(CardBodyFieldType.GENERAL)
                                                .setDescription(cardTextAccessor.getMessage("accountId.description", info.getAccountId()))
                                                .build()
                                )
                                .addField(
                                        new CardBodyField.Builder()
                                                .setTitle(cardTextAccessor.getMessage("regionName.title"))
                                                .setType(CardBodyFieldType.GENERAL)
                                                .setDescription(cardTextAccessor.getMessage("regionName.description", info.getRegionName()))
                                                .build()
                                )
                                .addField(
                                        new CardBodyField.Builder()
                                                .setTitle(cardTextAccessor.getMessage("certId.title"))
                                                .setType(CardBodyFieldType.GENERAL)
                                                .setDescription(cardTextAccessor.getMessage("certId.description", info.getCertIdentifier()))
                                                .build()
                                )
                                .build()
                )
                .addAction(approveAction.build())
                .addAction(dismissAction.build())
                .build();
    }

    @PostMapping(
            path = APPROVE_PATH,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<String>> approve(
            @RequestParam Map<String, String> params
    ) {
        logger.trace("approve called: params={}", params);

        String approvalUrl = params.get(APPROVAL_URL_PARAM);

        if (!validateUrl(approvalUrl)) {
            return Single.just(ResponseEntity.badRequest().body("Bad url: " + approvalUrl));
        }

        MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();

        params.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals(APPROVAL_URL_PARAM))
                .forEach(kvp -> formParams.add(kvp.getKey(), kvp.getValue()));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        ListenableFuture<ResponseEntity<String>> response = rest.exchange(
                approvalUrl,
                HttpMethod.POST,
                new HttpEntity<>(formParams, headers),
                String.class
        );

        return Async.toSingle(response);
    }

}
