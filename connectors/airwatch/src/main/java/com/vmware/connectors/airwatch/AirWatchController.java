/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch;

import com.vmware.connectors.airwatch.config.ManagedApp;
import com.vmware.connectors.airwatch.exceptions.GbAppMapException;
import com.vmware.connectors.airwatch.exceptions.ManagedAppNotFound;
import com.vmware.connectors.airwatch.exceptions.UdidException;
import com.vmware.connectors.airwatch.greenbox.GbApp;
import com.vmware.connectors.airwatch.greenbox.GbConnection;
import com.vmware.connectors.airwatch.service.AppConfigService;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardAction;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.payloads.response.CardActionKey;
import com.vmware.connectors.common.utils.CardTextAccessor;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.client.HttpClientErrorException;
import rx.Observable;
import rx.Single;

import javax.validation.Valid;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.vmware.connectors.common.utils.Async.toSingle;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.COOKIE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
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

    // Metadata includes connector regex derived from app keywords.
    private final String connectorMetadata;

    private final URI gbBaseUri;

    @Autowired
    public AirWatchController(AsyncRestOperations rest, CardTextAccessor cardTextAccessor,
                              AppConfigService appConfig, String connectorMetadata,
                              URI gbBaseUri) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.appConfig = appConfig;
        this.connectorMetadata = connectorMetadata;
        this.gbBaseUri = gbBaseUri;
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
        Set<String> appKeywords = cardRequest.getTokens("app_name");

        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, awAuth);

        Set<ManagedApp> apps = appKeywords.stream()
                .map(keyword -> appConfig.findManagedApp(keyword, clientPlatform))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return Observable.from(apps)
                .flatMap(app -> getCardForApp(headers, baseUrl, udid, app, routingPrefix, clientPlatform))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .map(ResponseEntity::ok)
                .toSingle();
    }

    @PostMapping(value = "/mdm/app/install", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public Single<ResponseEntity<HttpStatus>> installApp(
            @RequestHeader(name = AIRWATCH_AUTH_HEADER) String awAuth,
            @RequestParam("app_name") String appName,
            @RequestParam("udid") String udid,
            @RequestParam("platform") String platform) {

        ManagedApp app = appConfig.findManagedApp(appName, platform);
        if (app == null) {
            throw new ManagedAppNotFound("Can't install " + appName + ". It is not a managed app.");
        }

        String hznToken = awAuth.split("(?i)Bearer ")[1];
        return getEucToken(gbBaseUri, udid, platform, hznToken)
                .flatMap(eucToken -> getGbConnection(gbBaseUri, eucToken))
                .flatMap(gbConnection -> installApp(appName, gbConnection));
    }

    @ExceptionHandler({UdidException.class, ManagedAppNotFound.class, GbAppMapException.class})
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public Map<String, String> handleException(RuntimeException e) {
        logger.debug(e.getMessage());
        return Collections.singletonMap("error", e.getMessage());
    }

    private Observable<Card> getCardForApp(HttpHeaders headers, String baseUrl, String udid,
                                           ManagedApp app, String routingPrefix, String platform) {
        String appName = app.getName();
        String appBundle = app.getId();
        logger.debug("Getting app installation status for bundleId: {} with air-watch base url: {}",
                appBundle, baseUrl);
        ListenableFuture<ResponseEntity<JsonDocument>> future = rest.exchange(
                "{baseUrl}/deviceservices/AppInstallationStatus?Udid={udid}&BundleId={bundleId}",
                HttpMethod.GET, new HttpEntity<String>(headers), JsonDocument.class,
                baseUrl, udid, appBundle);
        return toSingle(future).toObservable()
                .onErrorResumeNext(throwable -> handleClientError(throwable, udid))
                .flatMap(entity -> getCard(entity.getBody(), routingPrefix,
                        appName, appBundle, udid, platform));

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

    private Observable<Card> getCard(JsonDocument installStatus, String routingPrefix,
                                     String appName, String appBundle, String udid, String platform) {

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

        CardAction.Builder appInstallActionBuilder =
                getInstallActionBuilder(routingPrefix, appName, udid, platform);

        cardBuilder
                .setName("AirWatch")
                .setTemplate(routingPrefix + "/templates/generic.hbs")
                .setHeader(cardTextAccessor.getHeader(appName), null)
                .setBody(cardBodyBuilder.build())
                .addAction(appInstallActionBuilder.build());
        return Observable.just(cardBuilder.build());
    }

    private CardAction.Builder getInstallActionBuilder(String routingPrefix,
                                                       String appName,
                                                       String udid,
                                                       String platform) {
        CardAction.Builder actionBuilder = new CardAction.Builder();
        actionBuilder.setLabel(cardTextAccessor.getActionLabel("installApp"))
                .setActionKey(CardActionKey.DIRECT)
                .setUrl(routingPrefix + "/mdm/app/install")
                .addRequestParam("app_name", appName)
                .addRequestParam("udid", udid)
                .addRequestParam("platform", platform)
                .setType(HttpMethod.POST);
        return actionBuilder;
    }

    private Single<String> getEucToken(URI baseUri, String udid, String platform, String hzn) {
        logger.trace("getEucToken called: GreenBox base url={}, udid={}, platform={}",
                gbBaseUri.toString(), udid, platform);

        HttpHeaders headers = new HttpHeaders();
        headers.set(COOKIE, "HZN=" + hzn);

        String deviceType = platform.replaceAll("(?i)ios", "Apple");

        ListenableFuture<ResponseEntity<JsonDocument>> future = rest.exchange(
                "{baseUrl}/catalog-portal/services/auth/eucTokens?deviceUdid={udid}&deviceType={deviceType}",
                HttpMethod.POST, new HttpEntity<String>(headers), JsonDocument.class,
                baseUri, udid, deviceType);

        return toSingle(future)
                .map(entity -> entity.getBody().read("$.eucToken"));
    }

    private Single<GbConnection> getGbConnection(URI gbBaseUri, String eucToken) {
        logger.trace("getGbConnection called: GreenBox base url={}", gbBaseUri.toString());
        return getCsrfToken(gbBaseUri, eucToken)
                .map(csrfToken -> new GbConnection(gbBaseUri, eucToken, csrfToken));
    }

    private Single<ResponseEntity<HttpStatus>> installApp(String gbAppName, GbConnection gbSession) {
        logger.trace("installApp called: GreenBox app name={} Base url={}", gbAppName, gbSession.getBaseUrl());
        return findGbApp(gbAppName, gbSession)
                .flatMap(gbApp -> installApp(gbApp, gbSession));
    }

    private Single<GbApp> findGbApp(String appName, GbConnection gbSession) {
        /*
         * Use search API to find GbApp by name.
         * Make sure response has only one entry.
         */
        logger.trace("findGbApp called: app name={} GreenBox={}", appName, gbSession.getBaseUrl());
        int RIGHT_APP_COUNT = 1;
        HttpHeaders headers = new HttpHeaders();
        headers.set(COOKIE, "USER_CATALOG_CONTEXT=" + gbSession.getEucToken());

        ListenableFuture<ResponseEntity<JsonDocument>> future = rest.exchange(
                "{baseUrl}/catalog-portal/services/api/entitlements?q={appName}",
                HttpMethod.GET, new HttpEntity<String>(headers),
                JsonDocument.class, gbSession.getBaseUrl(), appName);

        return toSingle(future)
                .map(entity -> {
                    JsonDocument document = entity.getBody();
                    JSONArray jsonArray = document.read("$._embedded.entitlements");
                    logger.debug("Found {} app(s) while searching GreenBox entitlements.", jsonArray.size());
                    if (jsonArray.size() != RIGHT_APP_COUNT) {
                        throw new GbAppMapException("Unable to map " + appName + " to a single GreenBox app");
                    }
                    return new GbApp(
                            document.read("$._embedded.entitlements[0].name"),
                            document.read("$._embedded.entitlements[0]._links.install.href"));
                });
    }

    private Single<ResponseEntity<HttpStatus>> installApp(GbApp gbApp, GbConnection gbSession) {
        /*
         * It triggers the native mdm app install.
         */
        logger.trace("installApp called: app name={} link={}", gbApp.getName(), gbApp.getInstallLink());
        HttpHeaders headers = new HttpHeaders();
        headers.set(COOKIE, "USER_CATALOG_CONTEXT=" + gbSession.getEucToken()
                + "; EUC_XSRF_TOKEN=" + gbSession.getCsrfToken());
        headers.set("X-XSRF-TOKEN", gbSession.getCsrfToken());
        ListenableFuture<ResponseEntity<JsonDocument>> future = rest.exchange(
                gbApp.getInstallLink(), HttpMethod.POST, new HttpEntity<String>(headers),
                JsonDocument.class);

        return toSingle(future)
                .map(entity -> {
                    String jobStatus = entity.getBody().read("$.status");
                    if ("PROCESSING".equals(jobStatus)) {
                        logger.debug("Install action submitted successfully with link {}.", gbApp.getInstallLink());
                    }
                    return entity.getStatusCode();
                })
                .map(ResponseEntity::new);
    }

    private Single<String> getCsrfToken(URI baseUri, String eucToken) {
        /*
         * Authenticated request to {GreenBox-Base-Url}/catalog-portal/ provides CSRF token.
         */
        logger.trace("getCsrfToken called: baseUri={}", baseUri.toString());
        HttpHeaders headers = new HttpHeaders();
        headers.set(COOKIE, "USER_CATALOG_CONTEXT=" + eucToken);
        ListenableFuture<ResponseEntity<String>> future = rest.exchange(
                "{baseUrl}/catalog-portal/", HttpMethod.OPTIONS, new HttpEntity<String>(headers),
                String.class, baseUri);
        return toSingle(future)
                .map(entity -> entity.getHeaders().getFirst("Set-Cookie"))
                .map(cookie -> cookie.split(";")[0].split("EUC_XSRF_TOKEN=")[1]);
    }
}
