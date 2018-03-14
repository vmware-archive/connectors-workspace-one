/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch;

import com.jayway.jsonpath.JsonPath;
import com.vmware.connectors.airwatch.config.ManagedApp;
import com.vmware.connectors.airwatch.exceptions.GbAppMapException;
import com.vmware.connectors.airwatch.exceptions.ManagedAppNotFound;
import com.vmware.connectors.airwatch.exceptions.UdidException;
import com.vmware.connectors.airwatch.exceptions.UnsupportedPlatform;
import com.vmware.connectors.airwatch.greenbox.GreenBoxApp;
import com.vmware.connectors.airwatch.greenbox.GreenBoxConnection;
import com.vmware.connectors.airwatch.service.AppConfigService;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.Reactive;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Created by harshas on 8/24/17.
 */
@RestController
public class AirWatchController {
    private static final Logger logger = LoggerFactory.getLogger(AirWatchController.class);

    private static final String AIRWATCH_AUTH_HEADER = "Authorization";
    private static final String AIRWATCH_BASE_URL_HEADER = "x-airwatch-base-url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";

    private static final int AW_USER_NOT_ASSOCIATED_WITH_UDID = 1001;
    private static final int AW_UDID_NOT_RESOLVED = 1002;

    private static final String APP_NAME_KEY = "app_name";
    private static final String UDID_KEY = "udid";
    private static final String PLATFORM_KEY = "platform";

    private final WebClient rest;

    private final CardTextAccessor cardTextAccessor;

    private final AppConfigService appConfig;

    // Metadata includes connector regex derived from app keywords.
    private final String connectorMetadata;

    private final URI gbBaseUri;

    @Autowired
    public AirWatchController(WebClient rest, CardTextAccessor cardTextAccessor,
                              AppConfigService appConfig, String connectorMetadata,
                              URI gbBaseUri) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.appConfig = appConfig;
        this.connectorMetadata = connectorMetadata;
        this.gbBaseUri = gbBaseUri;
    }

    @GetMapping(path = "/discovery/metadata.json")
    public ResponseEntity<String> getmetadata() {
        return ResponseEntity.ok(connectorMetadata);
    }

    @PostMapping(path = "/cards/requests",
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Cards>> getCards(
            @RequestHeader(name = AIRWATCH_AUTH_HEADER) String awAuth,
            @RequestHeader(name = AIRWATCH_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(name = ROUTING_PREFIX) String routingPrefix,
            Locale locale,
            @Valid @RequestBody CardRequest cardRequest) {

        String udid = cardRequest.getTokenSingleValue(UDID_KEY);
        String clientPlatform = cardRequest.getTokenSingleValue(PLATFORM_KEY);

        if (StringUtils.isAnyBlank(udid, clientPlatform)) {
            logger.debug("Either device UDID or client platform is blank.");
            return Mono.just(ResponseEntity.badRequest().build());
        }

        Set<String> appKeywords = cardRequest.getTokens("app_keywords");

        if (appKeywords == null) {
            logger.debug("Request is missing app_keywords token.");
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return Flux.fromIterable(appKeywords)
                .map(keyword -> appConfig.findManagedApp(keyword, clientPlatform))
                .filter(Optional::isPresent)
               .flatMap(app -> getCardForApp(awAuth, baseUrl, udid,
                        app.get(), routingPrefix, clientPlatform, locale))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .map(ResponseEntity::ok)
                .subscriberContext(Reactive.setupContext());
    }

    @PostMapping(value = "/mdm/app/install", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Void>> installApp(
            @RequestHeader(name = AIRWATCH_AUTH_HEADER) String awAuth,
            @RequestParam(APP_NAME_KEY) String appName,
            @RequestParam(UDID_KEY) String udid,
            @RequestParam(PLATFORM_KEY) String platform) {

        ManagedApp app = appConfig.findManagedApp(appName, platform)
                .orElseThrow(() -> new ManagedAppNotFound("Can't install " + appName + ". It is not a managed app."));

        logger.debug("Found managed app. {}:{} -> {}", platform, appName, app);

        String hznToken = awAuth.split("(?i)Bearer ")[1];
        return getEucToken(gbBaseUri, udid, platform, hznToken)
                .flatMap(eucToken -> getGbConnection(gbBaseUri, eucToken))
                .flatMap(greenBoxConnection -> installGbAppByName(appName, greenBoxConnection))
                .map(status -> ResponseEntity.status(OK).<Void>build())
                .subscriberContext(Reactive.setupContext());
    }

    @ExceptionHandler({UdidException.class, ManagedAppNotFound.class,
            GbAppMapException.class, UnsupportedPlatform.class})
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public Map<String, String> handleException(RuntimeException e) {
        logger.debug(e.getMessage());
        return Collections.singletonMap("error", e.getMessage());
    }

    private Flux<Card> getCardForApp(String awAuth, String baseUrl, String udid,
                                           ManagedApp app, String routingPrefix, String platform, Locale locale) {
        String appName = app.getName();
        String appBundle = app.getId();
        logger.debug("Getting app installation status for bundleId: {} with air-watch base url: {}",
                appBundle, baseUrl);
        return rest.get()
                .uri(baseUrl + "/deviceservices/AppInstallationStatus?Udid={udid}&BundleId={bundleId}", udid, appBundle)
                .header(AUTHORIZATION, awAuth)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> handleClientError(response, udid))
                .bodyToFlux(JsonDocument.class)
                .flatMap(Reactive.wrapFlatMapper(body -> getCard(body, routingPrefix, appName, appBundle, udid, platform, locale)));
    }

    private static Mono<Throwable> handleClientError(ClientResponse response, String udid) {
        return response.bodyToMono(String.class)
                .map(body -> {
                    if (!body.isEmpty()) {
                        Integer error = JsonPath.parse(body).read("$.Error");
                        if (Integer.valueOf(AW_USER_NOT_ASSOCIATED_WITH_UDID).equals(error)) {
                            return new UdidException("User is not associated with the UDID : " + udid);
                        } else if (Integer.valueOf(AW_UDID_NOT_RESOLVED).equals(error)) {
                            return new UdidException("Unable to resolve the UDID : " + udid);
                        }
                    }
                    Charset charset = response.headers().contentType()
                            .map(MimeType::getCharset)
                            .orElse(StandardCharsets.ISO_8859_1);
                    return new WebClientResponseException("Status error",
                            response.statusCode().value(),
                            response.statusCode().getReasonPhrase(),
                            response.headers().asHttpHeaders(), body.getBytes(), charset);

                });
    }

    private Flux<Card> getCard(JsonDocument installStatus, String routingPrefix,
                                     String appName, String appBundle, String udid, String platform, Locale locale) {

        Boolean isAppInstalled = Optional.<Boolean>ofNullable(
                installStatus.read("$.IsApplicationInstalled")).orElse(true);
        if (isAppInstalled) {
            logger.debug("App with bundleId: {} is already installed. No card is created.", appBundle);
            return Flux.empty();
        }
        // Create card for app install
        Card.Builder cardBuilder = new Card.Builder();
        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .setDescription(cardTextAccessor.getBody(locale));

        CardAction.Builder appInstallActionBuilder =
                getInstallActionBuilder(routingPrefix, appName, udid, platform, locale);

        cardBuilder
                .setName("AirWatch")
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(cardTextAccessor.getHeader(locale, appName))
                .setBody(cardBodyBuilder.build())
                .addAction(appInstallActionBuilder.build());
        return Flux.just(cardBuilder.build());
    }

    private CardAction.Builder getInstallActionBuilder(String routingPrefix,
                                                       String appName,
                                                       String udid,
                                                       String platform,
                                                       Locale locale) {
        CardAction.Builder actionBuilder = new CardAction.Builder();
        actionBuilder.setLabel(cardTextAccessor.getActionLabel("installApp", locale))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("installApp", locale))
                .setActionKey(CardActionKey.DIRECT)
                .setUrl(routingPrefix + "mdm/app/install")
                .addRequestParam(APP_NAME_KEY, appName)
                .addRequestParam(UDID_KEY, udid)
                .addRequestParam(PLATFORM_KEY, platform)
                .setType(HttpMethod.POST);
        return actionBuilder;
    }

    private Mono<String> getEucToken(URI baseUri, String udid, String platform, String hzn) {
        String deviceType = platform.replaceAll("(?i)ios", "Apple");

        return rest.post()
                .uri(baseUri + "/catalog-portal/services/auth/eucTokens?deviceUdid={udid}&deviceType={deviceType}", udid, deviceType)
                .cookie("HZN", hzn)
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .map(body -> body.read("$.eucToken"))
                .cast(String.class)
                .doOnEach(Reactive.wrapForItem(token-> logger.trace("Install app. Got EUC token: {}", token)));
        }

    private Mono<GreenBoxConnection> getGbConnection(URI gbBaseUri, String eucToken) {
         return getCsrfToken(gbBaseUri, eucToken)
                .map(csrfToken -> new GreenBoxConnection(gbBaseUri, eucToken, csrfToken))
                .doOnEach(Reactive.wrapForItem(gbc -> logger.trace("Install app. Got GB connection: {}", gbc)));
    }

    private Mono<String> installGbAppByName(
            String gbAppName, GreenBoxConnection gbSession) {
        return findGbApp(gbAppName, gbSession)
                .flatMap(gbApp -> installGbApp(gbApp, gbSession));
    }

    private Mono<GreenBoxApp> findGbApp(String appName, GreenBoxConnection gbSession) {
        /*
         * Use search API to find GreenBox app by name.
         * Make sure response has only one entry.
         * If by chance it finds more than one app which one should be selected to install ?
         */

        return rest.get()
                .uri(gbSession.getBaseUrl() + "/catalog-portal/services/api/entitlements?q={appName}", appName)
                .cookie("USER_CATALOG_CONTEXT", gbSession.getEucToken())
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .map(document -> toGreenBoxApp(document, appName))
                .doOnEach(Reactive.wrapForItem(gba -> logger.trace("Found GB app {} for {}", gba, appName)));
    }

    private GreenBoxApp toGreenBoxApp(JsonDocument document, String appName) {
        int RIGHT_APP_COUNT = 1;

        JSONArray jsonArray = document.read("$._embedded.entitlements");
        if (jsonArray.size() != RIGHT_APP_COUNT) {
            throw new GbAppMapException(
                    "Unable to map " + appName + " to a single GreenBox app");
        }
        return new GreenBoxApp(
                document.read("$._embedded.entitlements[0].name"),
                document.read("$._embedded.entitlements[0]._links.install.href"));
    }

    private Mono<String> installGbApp(GreenBoxApp gbApp, GreenBoxConnection gbSession) {
        /*
         * It triggers the native mdm app install.
         */
         return rest.post()
                .uri(gbApp.getInstallLink())
                .cookie("USER_CATALOG_CONTEXT", gbSession.getEucToken())
                .cookie("EUC_XSRF_TOKEN", gbSession.getCsrfToken())
                .header("X-XSRF-TOKEN", gbSession.getCsrfToken())
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .map(body -> body.read("$.status"))
                .cast(String.class)
                .doOnEach(Reactive.wrapForItem(status ->
                        logger.trace("Install action status: {} for {}", status, gbApp)));
    }

    private Mono<String> getCsrfToken(URI baseUri, String eucToken) {
        /*
         * Authenticated request to {GreenBox-Base-Url}/catalog-portal/ provides CSRF token.
         */
        logger.trace("getCsrfToken called: baseUri={}", baseUri.toString());
        return rest.options()
                .uri(baseUri + "/catalog-portal/")
                .cookie("USER_CATALOG_CONTEXT", eucToken)
                .exchange()
                .flatMap(Reactive::checkStatus)
                .map(response -> {
                    ResponseCookie cookie = response.cookies().getFirst("EUC_XSRF_TOKEN");
                    if (cookie == null) {
                        throw new IllegalStateException("No cookie found!");
                    }
                        return cookie.getValue();
                });
    }
}
