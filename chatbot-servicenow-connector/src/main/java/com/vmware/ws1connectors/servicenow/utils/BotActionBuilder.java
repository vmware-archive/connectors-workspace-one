/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.servicenow.bot.discovery.capabilities.OrderDesktop;
import com.vmware.ws1connectors.servicenow.bot.discovery.capabilities.OrderLaptop;
import com.vmware.ws1connectors.servicenow.bot.discovery.capabilities.OrderMobile;
import com.vmware.ws1connectors.servicenow.bot.discovery.capabilities.OrderTablet;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowCategory;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.domain.BotAction;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.domain.snow.CartItem;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.DEVICE_CATEGORY_URL;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.OPERATION_CANCEL_URL;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SHORT_DESCRIPTION;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.TYPE_KEY;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_TEXT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

public class BotActionBuilder {

    private static final String OPTION_NO = "option.no";
    private static final String OPTION_YES = "option.yes";
    private static final String ADD_ANOTHER = "add.another";
    private static final String NULL_ROUTING_PREFIX_MSG = "Routing Prefix";
    private static final String CATEGORY = "category";
    private static final String ROUTING_PREFIX = "routingPrefix";

    private final ConnectorTextAccessor connectorTextAccessor;

    public BotActionBuilder(ConnectorTextAccessor connectorTextAccessor) {
        this.connectorTextAccessor = connectorTextAccessor;
    }

    public BotAction confirmTaskCreate(String shortDescription, String routingPrefix, Locale locale, String createTaskApiUrl, String taskType) {
        return confirmationAction(routingPrefix, locale, createTaskApiUrl)
                .setRequestParam(TYPE_KEY, taskType)
                .setRequestParam(SHORT_DESCRIPTION, shortDescription)
                .setRequestHeaders(HttpHeaders.CONTENT_TYPE, APPLICATION_FORM_URLENCODED_VALUE)
                .setUrl(new Link(routingPrefix + createTaskApiUrl))
                .build();
    }

    public BotAction confirmCartCheckout(String routingPrefix, Locale locale, String checkoutURL) {
        return confirmationAction(routingPrefix, locale, checkoutURL)
                .build();
    }

    private BotAction.Builder confirmationAction(String routingPrefix, Locale locale, String url) {
        return new BotAction.Builder()
                .setTitle(connectorTextAccessor.getTitle(OPTION_YES, locale))
                .setDescription(connectorTextAccessor.getDescription(OPTION_YES, locale))
                .setType(HttpMethod.POST)
                .setUrl(new Link(routingPrefix + url));
    }

    public BotAction declineWorkflow(String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(connectorTextAccessor.getTitle(OPTION_NO, locale))
                .setDescription(connectorTextAccessor.getDescription(OPTION_NO, locale))
                .setType(HttpMethod.POST)
                .setUrl(new Link(routingPrefix + OPERATION_CANCEL_URL))
                .build();
    }

    public BotAction getEmptyCartBotAction(String routingPrefix, Locale locale, String emptyCart, HttpMethod delete, String cartApiUrl) {
        return new BotAction.Builder()
                .setTitle(connectorTextAccessor.getTitle(emptyCart, locale))
                .setDescription(connectorTextAccessor.getDescription(emptyCart, locale))
                .setType(delete)
                .setRequestHeaders(HttpHeaders.CONTENT_TYPE, APPLICATION_FORM_URLENCODED_VALUE)
                .setUrl(new Link(routingPrefix + cartApiUrl))
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
                .setTitle(connectorTextAccessor.getTitle(ADD_ANOTHER, locale))
                .setDescription(connectorTextAccessor.getDescription(ADD_ANOTHER, locale))
                .setType(HttpMethod.GET)
                .setUrl(new Link(routingPrefix + DEVICE_CATEGORY_URL))
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
        ArgumentUtils.checkArgumentNotNull(category, CATEGORY);

        switch (category) {
            case LAPTOP: return new OrderLaptop(connectorTextAccessor).getListOfLaptopsAction(routingPrefix, locale);
            case MOBILE: return new OrderMobile(connectorTextAccessor).getListOfMobilesAction(routingPrefix, locale);
            case DESKTOP: return new OrderDesktop(connectorTextAccessor).getListOfDesktopsAction(routingPrefix, locale);
            case TABLET: return new OrderTablet(connectorTextAccessor).getListOfTabletsAction(routingPrefix, locale);
            default: return null;
        }
    }
}
