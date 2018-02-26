/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.util.*;

public class MessageThread {
    private final static ObjectReader reader = new ObjectMapper().reader();
    private final static JsonPointer dataPointer = JsonPointer.compile("/data");

    @JsonProperty("messages")
    private final List<Message> messages = new ArrayList<>();

    public List<Message> getMessages() {
        return messages;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(64);
        stringBuilder.append("MessageThread: contains ")
                .append(messages.size())
                .append(" messages");
        for (Message msg : messages) {
            stringBuilder.append("\n\t").append(msg);
        }
        return stringBuilder.toString();
    }

    public static MessageThread parse(String json) throws IOException {
        // Point at "data" within larger body of JSON
        JsonNode data = reader.readTree(json).at(dataPointer);
        // Convert "data" to MessageThread
        return reader.treeToValue(data, MessageThread.class);
    }
}
