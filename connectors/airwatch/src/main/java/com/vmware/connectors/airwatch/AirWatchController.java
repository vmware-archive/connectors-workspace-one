/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch;

import com.vmware.connectors.airwatch.exceptions.UdidException;
import com.vmware.connectors.airwatch.service.AppConfigService;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardAction;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.utils.CardTextAccessor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.client.HttpClientErrorException;
import rx.Observable;
import rx.Single;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.vmware.connectors.common.utils.Async.toSingle;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Created by harshas on 8/24/17.
 */
@RestController
public class AirWatchController {
    private final static Logger logger = LoggerFactory.getLogger(AirWatchController.class);
    private static final String AIRWATCH_AUTH_HEADER = "Authorization";
    private static final String AIRWATCH_BASE_URL_HEADER = "x-airwatch-base-url";
    private final static String ROUTING_PREFIX = "x-routing-prefix";
    private final static String AW_USER_NOT_ASSOCIATED_WITH_UDID = "1001";
    private final static String AW_UDID_NOT_RESOLVED = "1002";

    private final AsyncRestOperations rest;

    private final CardTextAccessor cardTextAccessor;

    private final AppConfigService appConfig;

    // Metadata includes connector regex derived from app names.
    private final String connectorMetadata;

    @Autowired
    public AirWatchController(AsyncRestOperations rest, CardTextAccessor cardTextAccessor,
                              AppConfigService appConfig, String connectorMetadata) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.appConfig = appConfig;
        this.connectorMetadata = connectorMetadata;
    }

    @GetMapping(path = "/discovery/metadata.hal")
    public ResponseEntity<String> getmetadata() {
        return ResponseEntity.ok(connectorMetadata);
    }


    @PostMapping(path = "/cards/requests", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public Single<ResponseEntity<Cards>> getCards(
            @RequestHeader(name = AIRWATCH_AUTH_HEADER) String awAuth,
            @RequestHeader(name = AIRWATCH_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(name = ROUTING_PREFIX) String routingPrefix,
            @Valid @RequestBody CardRequest cardRequest) {

        String udid = cardRequest.getTokenSingleValue("udid");
        String clientPlatform = cardRequest.getTokenSingleValue("platform");
        Set<String> appNames = cardRequest.getTokens("app_name");

        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, awAuth);

        List<Pair<String, String>> apps = appNames.stream()
                .map(appName -> Pair.of(appName, appConfig.getAppId(clientPlatform, appName)))
                .filter(app -> app.getRight() != null)
                .collect(Collectors.toList());


        return Observable.from(apps)
                .flatMap(app -> getCardForApp(headers, baseUrl, udid, app, routingPrefix))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .map(ResponseEntity::ok)
                .toSingle();
    }

    @ExceptionHandler(UdidException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public Map<String, String> handleException(RuntimeException exception) {
        logger.debug(exception.getMessage());
        return Collections.singletonMap("error", exception.getMessage());
    }

    private Observable<Card> getCardForApp(HttpHeaders headers, String baseUrl, String udid,
                                           Pair<String, String> app, String routingPrefix) {
        String appName = app.getLeft();
        String appBundle = app.getRight();
        logger.debug("Getting app installation status for bundleId: {} with air-watch base url: {}",
                appBundle, baseUrl);
        ListenableFuture<ResponseEntity<JsonDocument>> future = rest.exchange(
                "{baseUrl}/deviceservices/AppInstallationStatus?Udid={udid}&BundleId={bundleId}",
                HttpMethod.GET, new HttpEntity<String>(headers), JsonDocument.class,
                baseUrl, udid, appBundle);
        return toSingle(future).toObservable()
                .onErrorResumeNext(throwable -> handleClientError(throwable, udid))
                .flatMap(entity -> getCard(entity.getBody(), routingPrefix, appName, appBundle));

    }

    private static Observable<ResponseEntity<JsonDocument>> handleClientError(Throwable throwable, String udid) {
        if (throwable instanceof HttpClientErrorException) {
            if (((HttpClientErrorException) throwable).getResponseBodyAsString()
                    .contains(AW_USER_NOT_ASSOCIATED_WITH_UDID)) {
                throw new UdidException("User is not associated with the UDID : " + udid);
            } else if (((HttpClientErrorException) throwable).getResponseBodyAsString()
                    .contains(AW_UDID_NOT_RESOLVED)) {
                throw new UdidException("Unable to resolve the UDID : " + udid);
            }
        }
        // If the problem is not because of UDID, let it bubble up.
        return Observable.error(throwable);
    }

    private Observable<Card> getCard(JsonDocument installStatus, String routingPrefix, String appName, String appBundle) {

        Boolean isAppInstalled = Optional.<Boolean>ofNullable(
                installStatus.read("$.IsApplicationInstalled")).orElse(true);
        if (isAppInstalled) {
            logger.debug("App with bundleId: {} is already installed. No card is created.", appBundle);
            return Observable.empty();
        }
        // Create card for app install
        Card.Builder cardBuilder = new Card.Builder();
        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .setDescription(cardTextAccessor.getBody());

        CardAction.Builder appInstallActionBuilder = getappInstallActionBuilder(appBundle);

        cardBuilder
                .setName("AirWatch")
                .setTemplate(routingPrefix + "/templates/generic.hbs")
                .setHeader(cardTextAccessor.getHeader(appName), null)
                .setBody(cardBodyBuilder.build())
                .addAction(appInstallActionBuilder.build());
        return Observable.just(cardBuilder.build());
    }

    private CardAction.Builder getappInstallActionBuilder(String appBundle) {
        CardAction.Builder actionBuilder = new CardAction.Builder();
        actionBuilder.setLabel(cardTextAccessor.getActionLabel("installApp"))
                .setActionKey("INSTALL_APP")
                .setUrl(cardTextAccessor.getMessage("ws1.installAppUrl", appBundle))
                .setType(HttpMethod.GET);
        return actionBuilder;
    }
}
