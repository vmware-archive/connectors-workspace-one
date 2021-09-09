/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.servicenow.BotFlowTest;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.domain.BotObjects;
import com.vmware.ws1connectors.servicenow.utils.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.servicenow.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.vmware.connectors.test.ControllerTestsBase.fromFile;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.CHECKOUT;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.EMPTY_CART;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.ITEM_DETAILS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTest {
    private static final String NO_BASE_URL = null;
    private static final String NO_ITEM_ID = null;
    private static final Integer NO_ITEM_QUANTITY = null;
    private static final String NO_AUTH = null;
    private static final String NO_ROUTING_PREFIX = null;
    private static final String ITEM_ID = "2ab7077237153000158bbfc8bcbe5da9";
    private static final String ITEM_ID_WITH_NO_ITEM_IN_CART = "3cecd2350a0a0a6a013a3a35a5e41c07";
    private static final Integer ITEM_QUANTITY = 1;
    private static final String AUTH = "Bearer test-GOOD-auth-token";
    private static final String ROUTING_PREFIX = "https://mf/connectors/abc123/botDiscovery/";
    private static final String BASE_URL = "http://localhost:54819/";
    private static final String BASE_URL_ADD_TO_CART = "https://mock-snow.com/";
    private static final String ADD_TO_CART_CONNECTOR_RESPONSE_WHEN_ITEM_ALREADY_IN_CART = "/botflows/connector/response/add_mac_to_cart.json";
    private static final String ADD_TO_CART_CONNECTOR_RESPONSE_WHEN_NO_ITEM_IN_CART = "/botflows/connector/response/add_mac_to_cart_when_no_item_in_cart.json";
    private static final String CLEAR_CART_SUCCESS_MSG = "Cart Cleared Successfully";
    private static final String CLEAR_CART_DESC = "There are no items in cart";
    private static final String LOOKUP_CART_RESP = "/botflows/servicenow/response/cart.json";
    private static final String LOOKUP_CART_RESP_WHEN_NO_EMPTY_CART = "/botflows/servicenow/response/empty_cart.json";
    private static final int BOT_OBJECT_WITH_SINGLE_ITEM = 1;
    private static final String CART_ACTION_ITEM = "cart.action.item";
    private static final String CART_ACTION_ITEM_MSG = "What would you like to do next?";
    private static final String EMPTY_CART_TITLE = "Empty Cart";
    private static final String EMPTY_CART_DESCRIPTION = "Empty everything in the cart.";
    private static final String CHECKOUT_TITLE = "Checkout";
    private static final String CHECKOUT_DESCRIPTION = "Checkout your cart.";
    private static final String ADD_ANOTHER = "add.another";
    private static final String ADD_ANOTHER_TITLE_MSG = "Add Another";
    private static final String ADD_ANOTHER_DESCRIPTION_MSG = "Add another item to cart";
    private static final String CART = "cart";
    private static final String CART_TITLE = "I have added Apple MacBook Pro to your cart.";
    private static final String CART_TITLE_WHEN_NO_ITEM_IN_CART = "I have added Dell XPS 13 to your cart.";
    private static final String CART_DESCRIPTION = "Things in your shopping cart.";
    private static final String CART_DESCRIPTION_EMPTY = "cart.description.empty";
    private static final String EMPTY_CART_DESCRIPTION_MSG = "Cart is empty.";
    private static final String CART_PROMPT_MSG = "cart.prompt.msg";
    private static final String CART_PROMPT_MSG_RESPONSE = "Here are the items currently in your cart:";
    private static final String APPLE_MAC_BOOK_PRO = "Apple MacBook Pro";
    private static final String DELL_XPS_13 = "Dell XPS 13";


    @Mock private WebClient mockRest;
    @Mock private WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock private WebClient.RequestBodySpec requestBodyMock;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriMock;
    @Mock private WebClient.ResponseSpec responseMock;
    @Mock private ExchangeFunction mockExchangeFunc;
    @Mock private ConnectorTextAccessor connectorTextAccessor;
    @Mock private ClientResponse clientResponse;

    @InjectMocks CartService cartService;

    @Test void testAddToCartWhenItemsAreAlreadyThereInCart() {
        Locale locale = null;
        mockBotAccessor();
        JsonDocument jsonDocument;
        URI baseUri = UriComponentsBuilder.fromUriString(BASE_URL_ADD_TO_CART).build().toUri();
        jsonDocument = new JsonDocument(Configuration.defaultConfiguration().jsonProvider().parse(FileUtils.readFileAsString(
                LOOKUP_CART_RESP)));
        mockLookupCart(jsonDocument);
        mockAddToCart();
        final Mono<BotObjects> addToCartMono = cartService.addToCart(ITEM_ID, ITEM_QUANTITY, AUTH, baseUri, ROUTING_PREFIX, locale);
        StepVerifier.create(addToCartMono)
                .expectNextMatches(botObjects -> isEquals(botObjects, ADD_TO_CART_CONNECTOR_RESPONSE_WHEN_ITEM_ALREADY_IN_CART))
                .verifyComplete();
    }

    private void mockBotAccessor() {
        when(connectorTextAccessor.getTitle(eq(CART_ACTION_ITEM), any())).thenReturn(
                CART_ACTION_ITEM_MSG);
        when(connectorTextAccessor.getDescription(eq(CART_ACTION_ITEM), any())).thenReturn(
                CART_ACTION_ITEM_MSG);
        when(connectorTextAccessor.getTitle(eq(EMPTY_CART), any())).thenReturn(EMPTY_CART_TITLE);
        when(connectorTextAccessor.getDescription(eq(EMPTY_CART), any())).thenReturn(EMPTY_CART_DESCRIPTION);
        when(connectorTextAccessor.getTitle(eq(CHECKOUT), any())).thenReturn(CHECKOUT_TITLE);
        when(connectorTextAccessor.getDescription(eq(CHECKOUT), any())).thenReturn(CHECKOUT_DESCRIPTION);
        when(connectorTextAccessor.getTitle(eq(ADD_ANOTHER), any())).thenReturn(ADD_ANOTHER_TITLE_MSG);
        when(connectorTextAccessor.getDescription(eq(ADD_ANOTHER), any())).thenReturn(ADD_ANOTHER_DESCRIPTION_MSG);
        when(connectorTextAccessor.getTitle(eq(CART), any(), eq(APPLE_MAC_BOOK_PRO))).thenReturn(CART_TITLE);
        when(connectorTextAccessor.getTitle(eq(CART), any(), eq(DELL_XPS_13))).thenReturn(CART_TITLE_WHEN_NO_ITEM_IN_CART);
        when(connectorTextAccessor.getDescription(eq(CART), any())).thenReturn(CART_DESCRIPTION);
        when(connectorTextAccessor.getMessage(eq(CART_DESCRIPTION_EMPTY), any())).thenReturn(EMPTY_CART_DESCRIPTION_MSG);
        when(connectorTextAccessor.getTitle(eq(CART_PROMPT_MSG), any(), any())).thenReturn(CART_PROMPT_MSG_RESPONSE);
        when(connectorTextAccessor.getDescription(eq(CART_PROMPT_MSG), any())).thenReturn(CART_PROMPT_MSG_RESPONSE);
    }

    private boolean isEquals(final BotObjects botObjects, String botObjectConnectorResponseFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        String responseAsJson;
        try {
            responseAsJson = BotFlowTest.normalizeBotObjects(objectMapper.writeValueAsString(botObjects));
            assertThat("objects should be identical", responseAsJson, sameJSONAs(fromFile(botObjectConnectorResponseFile)).allowingAnyArrayOrdering());
            return true;
        } catch (IOException e) {
            LOGGER.error("error while processing file");
        }
        return false;
    }

    private void mockLookupCart(JsonDocument jd) {
        when(mockRest.get()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(any(String.class))).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header(any(String.class), any(String.class))).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(JsonDocument.class)).thenReturn(Mono.just(jd));
    }

    @Test void testAddToCartWhenNoItemInCart() {
        Locale locale = null;
        JsonDocument jsonDocument;
        URI baseUri = UriComponentsBuilder.fromUriString(BASE_URL_ADD_TO_CART).build().toUri();
        jsonDocument = new JsonDocument(Configuration.defaultConfiguration().jsonProvider().parse(FileUtils.readFileAsString(
                LOOKUP_CART_RESP_WHEN_NO_EMPTY_CART)));
        mockLookupCart(jsonDocument);
        mockAddToCart();
        mockBotAccessor();
        final Mono<BotObjects> addToCartMono = cartService.addToCart(ITEM_ID_WITH_NO_ITEM_IN_CART, ITEM_QUANTITY, AUTH, baseUri, ROUTING_PREFIX, locale);
        StepVerifier.create(addToCartMono)
                .expectNextMatches(botObjects -> isEquals(botObjects, ADD_TO_CART_CONNECTOR_RESPONSE_WHEN_NO_ITEM_IN_CART))
                .verifyComplete();
    }

    private void mockAddToCart() {
        when(mockRest.post()).thenReturn(requestBodyUriMock);
        when(requestBodyUriMock.uri(any(String.class))).thenReturn(requestBodyMock);
        when(requestBodyMock.header(any(String.class), any(String.class))).thenReturn(requestBodyMock);
        when(requestBodyMock.bodyValue(any(Map.class))).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        when(responseMock.bodyToMono(Void.class)).thenReturn(Mono.empty());
    }

    private static Stream<Arguments> invalidInputsForAddToCart() {
        return new ArgumentsStreamBuilder()
                .add(NO_ITEM_ID, ITEM_QUANTITY, AUTH, BASE_URL, ROUTING_PREFIX)
                .add(ITEM_ID, NO_ITEM_QUANTITY, AUTH, BASE_URL, ROUTING_PREFIX)
                .add(ITEM_ID, ITEM_QUANTITY, NO_AUTH, BASE_URL, ROUTING_PREFIX)
                .add(ITEM_ID, ITEM_QUANTITY, AUTH, NO_BASE_URL, ROUTING_PREFIX)
                .add(ITEM_ID, ITEM_QUANTITY, AUTH, BASE_URL, NO_ROUTING_PREFIX)
                .build();
    }

    private static Stream<Arguments> invalidInputsForLookupCart() {
        return new ArgumentsStreamBuilder()
                .add(AUTH, BASE_URL, ROUTING_PREFIX, null)
                .add(NO_AUTH, BASE_URL, ROUTING_PREFIX, new CardRequest(null, null))
                .add(AUTH, NO_BASE_URL, ROUTING_PREFIX, new CardRequest(null, null))
                .add(AUTH, BASE_URL, NO_ROUTING_PREFIX, new CardRequest(null, null))
                .build();
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForAddToCart")
    void whenAddToCartProvidedWithInvalidInputs(final String itemId, final Integer itemQuantity, final String auth, final URI baseUri, final String routingPrefix) {
        Locale locale = null;
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> cartService.addToCart(itemId, itemQuantity, auth, baseUri, routingPrefix, locale));
        verify(mockExchangeFunc, never()).exchange(any(ClientRequest.class));
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForLookupCart")
    void whenLookupCartProvidedWithInvalidInputs(final String auth, final String baseUrl, final String routingPrefix, CardRequest cardRequest) {
        Locale locale = null;
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> cartService.lookupCart(auth, baseUrl, routingPrefix, locale, cardRequest, Optional.empty()));
        verify(mockExchangeFunc, never()).exchange(any(ClientRequest.class));
    }

    private static Stream<Arguments> invalidInputsForClearCart() {
        return new ArgumentsStreamBuilder()
                .add(NO_AUTH, BASE_URL)
                .add(AUTH, NO_BASE_URL)
                .add(NO_AUTH, NO_BASE_URL)
                .build();
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForClearCart")
    void whenClearCartProvidedWithInvalidInputs(final String auth, final String baseUri) {
        Locale locale = null;
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> cartService.emptyCart(auth, baseUri, locale));
        verify(mockExchangeFunc, never()).exchange(any(ClientRequest.class));
    }

    private void mockClearCartCart() {
        when(mockRest.delete()).thenReturn(requestHeadersUriMock);
        when(requestHeadersUriMock.uri(any(String.class))).thenReturn(requestHeadersMock);
        when(requestHeadersMock.header(any(String.class), any(String.class))).thenReturn(requestHeadersMock);
        when(requestHeadersMock.exchange()).thenReturn(Mono.just(clientResponse));
    }

    @Test void testClearCart() throws Exception {
        Locale locale = null;
        setupClearCartMock(locale);
        final Mono<ResponseEntity<BotObjects>> responseEntityMono =
                cartService.emptyCart(AUTH, BASE_URL, locale);
        StepVerifier.create(responseEntityMono)
                .expectNextMatches(this::isValidCartObject)
                .verifyComplete();
    }

    private void setupClearCartMock(Locale locale) throws Exception {
        when(clientResponse.statusCode()).thenReturn(HttpStatus.NO_CONTENT);
        when(connectorTextAccessor.getMessage(ServiceNowConstants.EMPTY_CART_SUCCESS_MSG, locale)).thenReturn(CLEAR_CART_SUCCESS_MSG);
        when(connectorTextAccessor.getMessage(ServiceNowConstants.EMPTY_CART_SUCCESS_DESC_MSG, locale)).thenReturn(
                CLEAR_CART_DESC);
        JsonDocument jsonDocument = new JsonDocument(Configuration.defaultConfiguration().jsonProvider().parse(FileUtils.readFileAsString(
                LOOKUP_CART_RESP)));
        mockLookupCart(jsonDocument);
        mockClearCartCart();
    }

    private boolean isValidCartObject(final ResponseEntity<BotObjects> responseEntity) {
        final BotObjects botObjects = responseEntity.getBody();
        assertThat(botObjects.getObjects()).hasSize(BOT_OBJECT_WITH_SINGLE_ITEM);
        BotItem botItem = botObjects.getObjects().get(0).get(ITEM_DETAILS);
        return CLEAR_CART_SUCCESS_MSG.equals(botItem.getTitle());
    }
}
