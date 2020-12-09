package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.connectors.common.utils.HashUtil;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardBanner {

    private final List<BannerItem> items;

    @JsonCreator
    public CardBanner(@JsonProperty("items") List<BannerItem> items) {
        this.items = items;
    }

    @JsonProperty("items")
    public List<BannerItem> getItems() {
        return items;
    }

    public String hash() {
        List<String> hashList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(items)) {
            items.forEach(item -> hashList.add(item == null ? "" : item.hash()));
        }
        return HashUtil.hash("items:", HashUtil.hashList(hashList));
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
