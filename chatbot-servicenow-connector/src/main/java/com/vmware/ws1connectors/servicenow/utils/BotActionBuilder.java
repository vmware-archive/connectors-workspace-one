/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.ws1connectors.servicenow.bot.discovery.capabilities.OrderDesktop;
import com.vmware.ws1connectors.servicenow.bot.discovery.capabilities.OrderLaptop;
import com.vmware.ws1connectors.servicenow.bot.discovery.capabilities.OrderMobile;
import com.vmware.ws1connectors.servicenow.bot.discovery.capabilities.OrderTablet;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowCategory;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.domain.BotAction;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.domain.snow.CartItem;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_TEXT;

public class BotActionBuilder {

    private static final String ADD_ANOTHER_ITEM_TITLE = "add.another.title";
    private static final String ADD_ANOTHER_ITEM_DESC = "add.another.description";
    private static final String NULL_ROUTING_PREFIX_MSG = "Routing Prefix";
    private static final String VALIDATOR_MSG = "Cannot be Null or blank: ";
    private static final String ROUTING_PREFIX = "routingPrefix";

    private final BotTextAccessor botTextAccessor;
    private final ServerProperties serverProperties;

    public BotActionBuilder(BotTextAccessor botTextAccessor, ServerProperties serverProperties) {
        this.botTextAccessor = botTextAccessor;
        this.serverProperties = serverProperties;
    }

    public BotAction getEmptyCartBotAction(String routingPrefix, Locale locale, String emptyCart, HttpMethod delete, String cartApiUrl) {
        return new BotAction.Builder()
                .setTitle(botTextAccessor.getActionTitle(emptyCart, locale))
                .setDescription(botTextAccessor.getActionDescription(emptyCart, locale))
                .setType(delete)
                .setRequestHeaders(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .setUrl(new Link(routingPrefix + ServiceNowConstants.SERVICE_NOW_CONNECTOR_CONTEXT_PATH + ServiceNowConstants.URL_PATH_SEPERATOR + cartApiUrl))
                .build();
    }

    public BotItem getChildObjectBotAction(String baseUrl, CartItem cartItem, String contextId) {
        return new BotItem.Builder()
                .setTitle(cartItem.getShortDescription())
                .setContextId(contextId)
                .setDescription(cartItem.getShortDescription())
                .setImage(getItemImageLink(baseUrl, cartItem.getPicture()))
                .setType(UI_TYPE_TEXT)
                .setWorkflowStep(WorkflowStep.INCOMPLETE)
                .build();
    }

    public BotAction getAddAnotherItemCartAction(String routingPrefix, Locale locale) {
        ArgumentUtils.checkArgumentNotBlank(routingPrefix, NULL_ROUTING_PREFIX_MSG);
        return new BotAction.Builder()
                .setTitle(botTextAccessor.getMessage(ADD_ANOTHER_ITEM_TITLE, locale))
                .setDescription(botTextAccessor.getMessage(ADD_ANOTHER_ITEM_DESC, locale))
                .setType(HttpMethod.GET)
                .setUrl(new Link(routingPrefix + ServiceNowConstants.SERVICE_NOW_CONNECTOR_CONTEXT_PATH + ServiceNowConstants.URL_PATH_SEPERATOR + ServiceNowConstants.DEVICE_CATEGORY_URL))
                .build();
    }

    private Link getItemImageLink(String baseUrl, String itemPicture) {
        // When there isn't a picture associated, it says - "picture": ""
        if (StringUtils.isBlank(itemPicture)) {
            return null;
        }
        return new Link(
                UriComponentsBuilder.fromUriString(baseUrl)
                        .replacePath(itemPicture)
                        .build()
                        .toUriString());
    }

    public BotAction buildBotActionForDeviceCategory(ServiceNowCategory category, String routingPrefix, Locale locale) {
        ArgumentUtils.checkArgumentNotBlank(routingPrefix, ROUTING_PREFIX);
        checkArgument(Objects.nonNull(category), new StringBuilder(VALIDATOR_MSG).append(category).toString());
        final String appContextPath = serverProperties.getServlet().getContextPath();
        switch (category) {
            case LAPTOP: return new OrderLaptop(botTextAccessor, appContextPath).getListOfLaptopsAction(routingPrefix, locale);
            case MOBILE: return new OrderMobile(botTextAccessor, appContextPath).getListOfMobilesAction(routingPrefix, locale);
            case DESKTOP: return new OrderDesktop(botTextAccessor, appContextPath).getListOfDesktopsAction(routingPrefix, locale);
            case TABLET: return new OrderTablet(botTextAccessor, appContextPath).getListOfTabletsAction(routingPrefix, locale);
            default: return null;
        }
    }
}
