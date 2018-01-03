/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.socialcast;

import com.vmware.connectors.common.model.Message;
import com.vmware.connectors.common.model.MessageThread;
import com.vmware.connectors.common.utils.CardTextAccessor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class handles a few related formatting tasks required by the Socialcast connector.
 */
public class SocialcastMessageFormatter {


    // The date format for the line that is prepended to each message, e.g.:
    // EmailThreadAboutSomething: Autoposted group created at 2017-01-01 12:34:56 EDT
    private static final DateTimeFormatter HEADER_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private static final Logger logger = LoggerFactory.getLogger(SocialcastMessageFormatter.class);

    private final CardTextAccessor cardTextAccessor;

    public SocialcastMessageFormatter(CardTextAccessor cta) {
        this.cardTextAccessor = cta;
    }


    /**
     * Create a name for the Socialcast group to be created from this message thread. The
     * group name contains the subject of the email thread and the date and time at which it
     * was posted to Socialcast.
     * <p>
     * Socialcast limits group names to a maximum of 140 characters. This method truncates
     * the name, if necessary, to meet that constraint.
     *
     * @param thread The MessageThread to be posted
     * @return A name for the Socialcast group, limited to 140 characters
     */
    public String makeGroupName(MessageThread thread) {
        // The group name is constructed by inserting (1) the subject line of the first email in the thread,
        // and (2) a formatted timestamp of when it was posted to Socialcast (i.e., now),
        // and inserting them into a string template from "text.properties" with the key "socialcast.header",
        // which might look something like:
        //     "{subject}: Autoposted group created at {date}"

        String firstThreadSubject = thread.getFirstSubject();
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);

        // However, Socialcast will not create a group with a title or description longer than 140 characters,
        // so we may need to truncate the title in some way before we send it.

        // First try the optimistic case, that the whole title will be < 140 chars; if so, return it and we're done.
        String untruncatedGroupName = cardTextAccessor.getHeader(firstThreadSubject, HEADER_DATE_FMT.format(utcNow));
        if (untruncatedGroupName.length() <= 140) {
            return untruncatedGroupName;
        }

        // Failing that, let's try to truncate the thread subject before we insert it into the template, so we don't
        // lose the timestamp at the end (assuming there is one). Can we ellipsize the subject down to a reasonable
        // length (arbitrarily, 30 chars) and still get under 140 for the whole title?
        int titleLengthWithoutSubject = untruncatedGroupName.length() - firstThreadSubject.length();
        if (titleLengthWithoutSubject < 110) {
            String truncatedSubject = StringUtils.abbreviate(firstThreadSubject, 140 - titleLengthWithoutSubject);
            return cardTextAccessor.getHeader(truncatedSubject, HEADER_DATE_FMT.format(utcNow));
        }

        // If *that* doesn't work, the template for the group title is too long even without the subject in it.
        // All we can do is ellipsize the whole title and log a warning so that maybe someone will fix the problem
        logger.warn("The template for Socialcast group titles, \"{}\", is too long. Please reduce it to about 90 characters in length.",
                cardTextAccessor.getHeader("{0}", "{1}"));
        return StringUtils.abbreviate(cardTextAccessor.getHeader(thread.getFirstSubject(), HEADER_DATE_FMT.format(utcNow)), 140);
    }

    /**
     * Create a description for the Socialcast group to be created from this message thread. The
     * group name contains the name of the thread's originator and the date and time at which the
     * first message in the thread was sent.
     * <p>
     * Socialcast limits group descriptions to a maximum of 140 characters. This method truncates
     * the description, if necessary, to meet that constraint.
     *
     * @param thread The MessageThread to be posted
     * @return A description for the Socialcast group, limited to 140 characters
     */
    public String makeGroupDescription(MessageThread thread) {
        // The group description is constructed by inserting (1) the name or email of the originator of the thread,
        // and (2) a formatted timestamp of when the thread was started,
        // and inserting them into a string template from "text.properties" with the key "socialcast.body",
        // which might look something like:
        //     "Messages from an email thread created by {sender} beginning {date}."

        // Get the sender's name if available, or at least their address
        String firstSenderName = thread.getFirstSender().getName();
        if (StringUtils.isBlank(firstSenderName)) {
            firstSenderName = thread.getFirstSender().getEmailAddress();
        }

        String threadStartDate = HEADER_DATE_FMT.format(thread.getFirstSentDate());

        // However, Socialcast will not create a group with a title or description longer than 140 characters,
        // so we may need to truncate the description in some way before we send it.

        // First try the optimistic case, that the whole description will be < 140 chars; if so, return it and we're done.
        String untruncatedGroupDesc = cardTextAccessor.getBody(firstSenderName, threadStartDate);
        if (untruncatedGroupDesc.length() <= 140) {
            return untruncatedGroupDesc;

        } else {
            // If it's over 140 characters, just ellipsize it and log a warning
            logger.warn("The template for Socialcast group descriptions, \"{}\", is too long. Please reduce it to about 90 characters in length.",
                    cardTextAccessor.getBody("{0}", "{1}"));
            return StringUtils.abbreviate(untruncatedGroupDesc, 140);
        }
    }

    /**
     * This method prepends some metadata to the text of an email message to show the message's sender,
     * subject, and date sent.
     *
     * @param msg The message to be formatted
     * @return The message's text with "Sent by", "Subject", and "Date" headers prepended
     */
    public String formatMessageForDisplay(Message msg) {
        String messageFrom;
        if (msg.getSender().getName() == null) {
            messageFrom = msg.getSender().getEmailAddress();
        } else {
            messageFrom = msg.getSender().getName() + " <" + msg.getSender().getEmailAddress() + ">";
        }

        return cardTextAccessor.getActionLabel("displayheaderfrom", messageFrom)
                + '\n'
                + cardTextAccessor.getActionLabel("displayheaderdate", HEADER_DATE_FMT.format(msg.getSentDate()))
                + '\n'
                + cardTextAccessor.getActionLabel("displayheadersubject", msg.getSubject())
                + '\n'
                + '\n'
                + msg.getText();
    }


}
