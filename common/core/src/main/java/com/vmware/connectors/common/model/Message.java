/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.CRC32;

@SuppressWarnings("PMD.ShortVariable")
public class Message {

    @JsonProperty("id")
    private String id;
    @JsonProperty("sender")
    private UserRecord sender;
    @JsonProperty("recipients")
    private List<UserRecord> recipients;
    @JsonProperty("subject")
    private String subject;
    @JsonProperty("text")
    private String text;
    @JsonProperty("date")
    private ZonedDateTime sentDate;

    public String getId() {
        if (StringUtils.isNotBlank(id)) {
            return id;
        } else {
            return getSurrogateId();
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserRecord getSender() {
        return sender;
    }

    public void setSender(UserRecord sender) {
        this.sender = sender;
    }

    public List<UserRecord> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<UserRecord> recipients) {
        this.recipients = recipients;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ZonedDateTime getSentDate() {
        return sentDate;
    }

    public void setSentDate(ZonedDateTime sentDate) {
        this.sentDate = sentDate;
    }

    public void setSentDate(String dateString) {
        this.sentDate = ZonedDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(dateString));
    }

    public String toString() {
        return String.format("Message %s: subj <<%s>>, sender <<%s>>, body length %d chars", id, subject, sender, (text == null) ? 0 : text.length());
    }

    // The intent of this method is to generate a more-or-less unique value corresponding to the content of this email
    // in a way that a client could replicate independently; for this reason, Java's hashCode() methods are not used
    private String getSurrogateId() {
        String fingerprint = getSubject() + ':' + getSender().getEmailAddress() + ':' + getSentDate().toString();
        CRC32 checksummer = new CRC32();
        checksummer.update(fingerprint.getBytes());
        return Long.toHexString(checksummer.getValue());
    }
}
