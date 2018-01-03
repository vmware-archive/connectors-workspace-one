/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.github.pr.v3;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

/**
 * https://developer.github.com/v3/pulls/reviews/
 */
@AutoProperty
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Review {

    @JsonProperty("body")
    private String body;

    @JsonProperty("event")
    private EventType event;


    public enum EventType {
        APPROVE,
        COMMENT,
        REQUEST_CHANGES;
    }


    public static Review approve() {
        Review review = new Review();
        review.setEvent(EventType.APPROVE);
        return review;
    }

    public static Review comment(String comment) {
        Review review = new Review();
        review.setEvent(EventType.COMMENT);
        review.setBody(comment);
        return review;
    }

    public static Review requestChanges(String request) {
        Review review = new Review();
        review.setEvent(EventType.REQUEST_CHANGES);
        review.setBody(request);
        return review;
    }


    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public EventType getEvent() {
        return event;
    }

    public void setEvent(EventType event) {
        this.event = event;
    }


    @Override
    public String toString() {
        return Pojomatic.toString(this);
    }

}
