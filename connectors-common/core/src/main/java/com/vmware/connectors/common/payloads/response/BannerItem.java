package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.vmware.connectors.common.utils.HashUtil;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BannerItem {

    private final Type type;
    private final String href;
    private final String title;
    private final String description;

    @JsonCreator
    public BannerItem(
            @JsonProperty("type") Type type,
            @JsonProperty("href") String href,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description
    ) {
        this.type = type;
        this.href = href;
        this.title = title;
        this.description = description;
    }

    @JsonProperty("type")
    public Type getType() {
        return type;
    }

    @JsonProperty("href")
    public String getHref() {
        return href;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public String hash() {
        return HashUtil.hash(
                "type:", type,
                "href:", href,
                "title:", title,
                "description:", description
        );
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public enum Type {

        VIDEO("video"),
        IMAGE("image");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static Type fromString(String type) {
            for (Type t : values()) {
                if (t.value.equals(type)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Unknown string value for banner item type");
        }

    }

}
