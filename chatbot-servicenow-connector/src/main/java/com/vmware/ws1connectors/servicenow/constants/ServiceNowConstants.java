/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.constants;

public final class ServiceNowConstants {

    public static final int MAX_NO_OF_RECENT_TICKETS_TO_FETCH = 5;
    public static final String CHILDREN = "children";
    public static final String OBJECTS = "objects";
    public static final String VIEW_TASK_MSG_PROPS = "view.task.msg";
    public static final String VIEW_TASK_TITLE = "view.task.title";
    public static final String AUTH_HEADER = "X-Connector-Authorization";
    public static final String BASE_URL_HEADER = "X-Connector-Base-Url";
    public static final String ROUTING_PREFIX = "x-routing-prefix";
    public static final String ROUTING_PREFIX_TEMPLATE = "X-Routing-Template";

    public static final String SNOW_CATALOG_ENDPOINT = "/api/sn_sc/servicecatalog/catalogs";
    public static final String SNOW_CATALOG_CATEGORY_ENDPOINT = "/api/sn_sc/servicecatalog/catalogs/{catalog_id}";
    public static final String SNOW_ADD_TO_CART_ENDPOINT = "/api/sn_sc/servicecatalog/items/{item_id}/add_to_cart";
    public static final String SNOW_CHECKOUT_ENDPOINT = "/api/sn_sc/servicecatalog/cart/checkout";
    public static final String SNOW_DELETE_FROM_CART_ENDPOINT = "/api/sn_sc/servicecatalog/cart/{cart_item_id}";
    public static final String SNOW_DELETE_CART_ENDPOINT = "/api/sn_sc/servicecatalog/cart/{cart_id}/empty";
    public static final String SNOW_DELETE_TASK_ENDPOINT = "/api/now/table/task/{task_id}";
    public static final String CREATE_TASK_URL = "api/v1/task/create";
    public static final String VIEW_TASK_URL = "api/v1/tasks";
    public static final String SNOW_CATEGORY_ITEM_URL = "/api/sn_sc/servicecatalog/items";
    public static final String SNOW_CART_URL = "/api/sn_sc/servicecatalog/cart";

    public static final String DEVICE_CATEGORY_URL = "api/v1/deviceCategoryList";
    public static final String DEVICE_LIST_URL = "api/v1/device_list";
    public static final String ITEM_BY_CATEGORY_LIMIT = "10";
    public static final String ITEM_BY_CATEGORY_OFFSET = "0";
    public static final String CART_API_URL = "api/v1/cart";
    public static final String CHECKOUT_URL = "api/v1/checkout";
    public static final String DEL_CART_URL = "api/v1/cart/{cartItemId}";

    public static final String SNOW_SYS_PARAM_LIMIT = "sysparm_limit";
    public static final String SNOW_SYS_PARAM_TEXT = "sysparm_text";
    public static final String SNOW_SYS_PARAM_CAT = "sysparm_category";
    public static final String SNOW_SYS_PARAM_QUAN = "sysparm_quantity";
    public static final String SNOW_SYS_PARAM_OFFSET = "sysparm_offset";
    public static final String NUMBER = "number";
    public static final String INSERT_OBJECT_TYPE = "INSERT_OBJECT_TYPE";
    public static final String ITEM_DETAILS = "itemDetails";
    public static final String ACTION_RESULT_KEY = "result";
    public static final String OBJECT_TYPE_BOT_DISCOVERY = "botDiscovery";
    public static final String OBJECT_TYPE_TASK = "task";
    public static final String OBJECT_TYPE_CART = "cart";
    public static final String CONTEXT_ID = "contextId";

    public static final String CATALOG_TITLE = "Service Catalog";
    public static final String CONFIG_FILE_TICKET_TABLE_NAME = "file_ticket_table_name";
    public static final String NO_OPEN_TICKETS_MSG = "noOpenTicketsMsg";
    public static final String TEXT = "text";

    public static final String ITEM_ID = "itemId";
    public static final int MIN_LENGTH_TEXT_AREA = 1;
    public static final String CATALOG_ID = "catalog_id";
    public static final String CART_ID = "cart_id";
    public static final String MESSAGE = "message";

    public static final String TITLE = ".title";
    public static final String DESCRIPTION = ".description";
    public static final String LABEL = ".label";
    public static final String ADD_TO_CART = "addToCart";
    public static final String ITEM_COUNT = "itemCount";
    public static final String TEXT_AREA = "textarea";
    public static final String CHECKOUT = "checkout";
    public static final String EMPTY_CART = "emptyCart";
    public static final String REMOVE_FROM_CART = "removeFromCart";
    public static final String CART_RESPONSE_JSON_PATH = "$.result.*.items.*";
    public static final String CART_OBJ_TYPE_DESC = ".description.empty";
    public static final String EMPTY_CART_SUCCESS_MSG = "empty.cart.action.success";
    public static final String EMPTY_CART_SUCCESS_DESC_MSG = "empty.cart.desc";
    public static final String EMPTY_CART_ERROR_MSG = "empty.cart.error";
    public static final String ERROR_MSG = "$.error.message";
    public static final String CART_ID_JSON_PATH = "$.result.cart_id";
    public static final String ITEM_ID_STR = "item_id";
    public static final String SERVICE_NOW_CONNECTOR_CONTEXT_PATH = "servicenow-connector";
    public static final String URL_PATH_SEPERATOR = "/";
    public static final String DEVICE_CATEGORY = "device_category";
    public static final String UI_TYPE_BUTTON = "button";
    public static final String UI_TYPE_ITEM = "item";
    public static final String UI_TYPE_STATUS = "status";
    public static final String UI_TYPE_CONFIRMATION = "confirmation";
    public static final String UI_TYPE_TEXT = "text";
    public static final String CONTEXT_PATH_TEMPLATE = "CONTEXT_PATH";
    public static final String TASK_DO = "task.do";
    public static final String SYS_ID = "sys_id";
    public static final String IMPACT = "impact";
    public static final String STATUS = "status";
    public static final String SHORT_DESCRIPTION = "shortDescription";
    public static final String TICKET_NO = "ticketNo";
    public static final String PRICE = "Price";

    private ServiceNowConstants() { }

}
