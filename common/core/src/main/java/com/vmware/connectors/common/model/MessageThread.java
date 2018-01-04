/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessageThread {
    private final static ObjectReader reader = new ObjectMapper().reader();
    private final static JsonPointer dataPointer = JsonPointer.compile("/data");
    @JsonProperty("messages")
    private final List<Message> messages = new ArrayList<>();

    @JsonIgnore
    public Set<UserRecord> allUsers() {
        return new HashSet<>(allUsersByEmail().values());
    }

    @JsonIgnore
    public Map<String, UserRecord> allUsersByEmail() {
        Map<String, UserRecord> emailMap = new HashMap<>();
        for (Message m : messages) {
            UserRecord sender = m.getSender();
            emailMap.put(sender.getEmailAddress(), sender);
            for (UserRecord recipient : m.getRecipients()) {
                emailMap.put(recipient.getEmailAddress(), recipient);
            }
        }
        return emailMap;
    }

    public List<Message> getMessages() {
        return messages;
    }

    @JsonIgnore
    public String getFirstSubject() {
        if (messages.isEmpty()) {
            return null;
        } else {
            return messages.get(0).getSubject();
        }
    }

    @JsonIgnore
    public UserRecord getFirstSender() {
        if (messages.isEmpty()) {
            return null;
        } else {
            return messages.get(0).getSender();
        }
    }

    @JsonIgnore
    public ZonedDateTime getFirstSentDate() {
        if (messages.isEmpty()) {
            return null;
        } else {
            return messages.get(0).getSentDate();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("MessageThread: contains ")
                .append(messages.size())
                .append(" messages");
        for (Message msg : messages) {
            sb.append("\n\t").append(msg);
        }
        return sb.toString();
    }

    public static MessageThread parse(String json) throws IOException {
        // Point at "data" within larger body of JSON
        JsonNode data = reader.readTree(json).at(dataPointer);
        // Convert "data" to MessageThread
        return reader.treeToValue(data, MessageThread.class);
    }
}
