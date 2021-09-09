/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.catalog.category.api.response.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.ws1connectors.servicenow.utils.HtmlToPlainTextConverter;
import lombok.Getter;

import java.util.List;

@Getter
@SuppressWarnings("PMD.TooManyFields")
public class CategoryItem {
    @JsonProperty("short_description")
    private String shortDescription;
    @JsonProperty("kb_article")
    private String kbArticle;
    @JsonProperty("icon")
    private String icon;
    @JsonProperty("description")
    private String description;
    @JsonProperty("mandatory_attachment")
    private boolean mandatoryAttachment;
    @JsonProperty("request_method")
    private String requestMethod;
    @JsonProperty("type")
    private String type;
    @JsonProperty("visible_standalone")
    private boolean visibleStandalone;
    @JsonProperty("local_currency")
    private String localCurrency;
    @JsonProperty("sys_class_name")
    private String sysClassName;
    @JsonProperty("sys_id")
    private String sysId;
    @JsonProperty("content_type")
    private String contentType;
    @JsonProperty("price")
    private String price;
    @JsonProperty("recurring_frequency")
    private String recurringFrequency;
    @JsonProperty("price_currency")
    private String priceCurrency;
    @JsonProperty("order")
    private Integer order;
    @JsonProperty("show_price")
    private boolean showPrice;
    @JsonProperty("recurring_price")
    private String recurringPrice;
    @JsonProperty("show_quantity")
    private boolean showQuantity;
    @JsonProperty("picture")
    private String picture;
    @JsonProperty("url")
    private String url;
    @JsonProperty("recurring_price_currency")
    private String recurringPriceCurrency;
    @JsonProperty("localized_price")
    private String localizedPrice;
    @JsonProperty("catalogs")
    private List<CatalogItem> catalogs;
    @JsonProperty("name")
    private String name;
    @JsonProperty("localized_recurring_price")
    private String localizedRecurringPrice;
    @JsonProperty("show_wishlist")
    private boolean showWishlist;
    @JsonProperty("category")
    private Category category;
    @JsonProperty("show_delivery_time")
    private boolean showDeliveryTime;

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = HtmlToPlainTextConverter.toPlainText(description);
    }
}
