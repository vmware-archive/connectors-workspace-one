package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.connectors.common.utils.HashUtil;

import java.time.OffsetDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class Sticky {

    private final OffsetDateTime until;
    private final String type;

    @JsonCreator
    public Sticky(
            @JsonProperty("until") OffsetDateTime until,
            @JsonProperty("type") String type
    ) {
        this.until = until;
        this.type = type;
    }

    @JsonProperty("until")
    public OffsetDateTime getUntil() {
        return until;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public String hash() {
        return HashUtil.hash(
                "until: ", this.until.toEpochSecond(),
                "type: ", this.type
        );
    }

}
