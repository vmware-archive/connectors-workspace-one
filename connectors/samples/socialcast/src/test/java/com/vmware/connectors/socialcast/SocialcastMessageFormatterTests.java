/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.socialcast;

import com.vmware.connectors.common.model.Message;
import com.vmware.connectors.common.model.MessageThread;
import com.vmware.connectors.common.model.UserRecord;
import com.vmware.connectors.common.utils.CardTextAccessor;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.test.context.TestPropertySource;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestPropertySource(locations = "classpath:application-test.properties")
public class SocialcastMessageFormatterTests {

    private SocialcastMessageFormatter fmtr;

    private MessageThread mockThread;

    private TestMessageSource testMessageSource;

    @Before
    public void setup() throws Exception {
        testMessageSource = new TestMessageSource();

        fmtr = new SocialcastMessageFormatter(new CardTextAccessor(testMessageSource));

        mockThread = mock(MessageThread.class);
    }


    @Test
    public void testGroupNameNormalCase() {
        // This test covers the case in which the template and subject are both short enough that they can be combined
        // without exceeding 140 characters. In this case, no truncation or ellipsizing is expected.

        when(mockThread.getFirstSubject()).thenReturn("Subject789");  // 10 chars

        testMessageSource.addMessage("header", "{0}: 01234567890123456789 {1}"); // 23 chars, excluding template vars

        String groupName = fmtr.makeGroupName(mockThread);

        assertThat(groupName.length(), is(54));  // length is ok
        assertThat(groupName, startsWith("Subject789"));  // starts with the subject
        assertThat(groupName, endsWith("Z"));  // ends with the timestamp
        assertThat(groupName, not(containsString("...")));  // no ellipses
    }


    @Test
    public void testGroupNameSubjectTooBig() {
        // This test covers the case in which the template for the group name is of reasonable size, but adding the
        // subject and date result in a string longer than 140 characters. The subject is "long enough to truncate
        // meaningfully", i.e., longer than 30 characters.
        // In this case, the message formatter is expected to ellipsize the subject to a sufficient length that it and
        // the date can be inserted into the template without exceeding 140 characters. The message is not ellipsized
        // in this case, so the date stamp should still appear at the end.

        when(mockThread.getFirstSubject()).thenReturn("Subject789012345678901234567890123456789012345678901234567890123456789");  // 70 chars

        testMessageSource.addMessage("header", "{0}: 01234567890123456789012345678901234567890123456789012345678901234567890123456789 {1}"); // 83 chars, excluding template vars

        String groupName = fmtr.makeGroupName(mockThread);

        assertThat(groupName.length(), lessThanOrEqualTo(140));  // length is ok
        assertThat(groupName, endsWith("Z"));  // ends with the timestamp, not a ellipsis
    }


    @Test
    public void testGroupNameBigTemplateWithSubjectTooSmallToTruncate() {
        // This test covers the case in which the template for the group name is of reasonable size, but adding the
        // subject and date result in a string longer than 140 characters. The subject is "too short to truncate
        // meaningfully", i.e., less than 30 characters.
        // In this case, the message formatter is expected to insert the subject and date into the template and then
        // ellipsize the result to a size of <= 140 characters.

        when(mockThread.getFirstSubject()).thenReturn("Subject7890123456789");  // 20 chars

        testMessageSource.addMessage("header", "{0}: 012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789 {1}"); // 123 chars, excluding template vars

        String groupName = fmtr.makeGroupName(mockThread);

        assertThat(groupName.length(), lessThanOrEqualTo(140));  // length is ok
        assertThat(groupName, endsWith("..."));  // ends with an ellipsis
    }

    @Test
    public void testGroupNameTemplateTooBig() {
        // This test covers the case in which the template for the group name is longer than 140 characters all by itself.
        // In this case, the message formatter is expected to insert the subject and date into the template and then
        // ellipsize the result to a size of <= 140 characters.

        when(mockThread.getFirstSubject()).thenReturn("Subject890");

        testMessageSource.addMessage("header", "{0}: 01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789 {1}");

        String groupName = fmtr.makeGroupName(mockThread);

        assertThat(groupName, startsWith("Subject890"));  // starts with the subject
        assertThat(groupName.length(), lessThanOrEqualTo(140));  // length is ok
        assertThat(groupName, endsWith("..."));  // ends with an ellipsis
    }

    @Test
    public void testGroupDescNormalCase() {
        // This test covers the case in which the template and sender's name are both short enough that they can be combined
        // without exceeding 140 characters. In this case, no truncation or ellipsizing is expected.
        UserRecord johnSmith = new UserRecord();
        johnSmith.setName("John Smith");
        johnSmith.setEmailAddress("john@smiths.org");
        when(mockThread.getFirstSender()).thenReturn(johnSmith);

        when(mockThread.getFirstSentDate()).thenReturn(ZonedDateTime.parse("2001-01-01T00:00:00Z"));

        testMessageSource.addMessage("body", "Messages from an email thread created by {0} beginning {1}");

        String groupDesc = fmtr.makeGroupDescription(mockThread);

        assertThat(groupDesc, containsString("John Smith"));
        assertThat(groupDesc.length(), lessThanOrEqualTo(140));  // length is ok
        assertThat(groupDesc, endsWith("Z"));  // ends with the timestamp
    }


    @Test
    public void testGroupDescUserNameTooBig() {
        // This test covers the case in which the template for the group description is of reasonable size, but adding the
        // sender and date result in a string longer than 140 characters.
        // In this case, the message formatter is expected to insert the sender's name and date into the template and then
        // ellipsize the result to a size of <= 140 characters.
        UserRecord johnSmith = new UserRecord();
        johnSmith.setName("John456789Smith5678901234567890123456789012345678901234567890123456789012345678901234567890123456789");  // 100 chars
        johnSmith.setEmailAddress("john@smiths.org");
        when(mockThread.getFirstSender()).thenReturn(johnSmith);

        when(mockThread.getFirstSentDate()).thenReturn(ZonedDateTime.parse("2001-01-01T00:00:00Z"));

        testMessageSource.addMessage("body", "Messages from an email thread created by {0} beginning {1}");

        String groupDesc = fmtr.makeGroupDescription(mockThread);

        assertThat(groupDesc, containsString("John456789Smith567890"));
        assertThat(groupDesc.length(), lessThanOrEqualTo(140));  // length is ok
        assertThat(groupDesc, endsWith("..."));  // ends with an ellipsis
    }


    @Test
    public void testGroupDescTemplateTooBig() {
        // This test covers the case in which the template for the group description is longer than 140 characters all by itself.
        // In this case, the message formatter is expected to insert the sender's name and date into the template and then
        // ellipsize the result to a size of <= 140 characters.
        UserRecord johnSmith = new UserRecord();
        johnSmith.setName("John Smith");
        johnSmith.setEmailAddress("john@smiths.org");
        when(mockThread.getFirstSender()).thenReturn(johnSmith);

        when(mockThread.getFirstSentDate()).thenReturn(ZonedDateTime.parse("2001-01-01T00:00:00Z"));

        testMessageSource.addMessage("body", "{0}: 01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789 {1}");

        String groupDesc = fmtr.makeGroupDescription(mockThread);

        assertThat(groupDesc, startsWith("John Smith"));
        assertThat(groupDesc.length(), lessThanOrEqualTo(140));  // length is ok
        assertThat(groupDesc, endsWith("..."));  // ends with ellipsis

    }

    @Test
    public void testGroupDescNoUserName() {
        // This test covers the case in which the sender's UserRecord contains no name, only an email address.
        // In this case, the formatter should insert the sender's email address into the group description
        // in lieu of the sender's name.
        String uucpEmailAddress = "uunet!rice!mailhost!{seismo, ut-sally, ihnp4}!beta!gamma!me.UUCP";  // remember these?
        UserRecord johnSmith = new UserRecord();
        johnSmith.setEmailAddress(uucpEmailAddress);
        when(mockThread.getFirstSender()).thenReturn(johnSmith);

        when(mockThread.getFirstSentDate()).thenReturn(ZonedDateTime.parse("2001-01-01T00:00:00Z"));

        testMessageSource.addMessage("body", "Messages from an email thread created by {0} beginning {1}");

        String groupDesc = fmtr.makeGroupDescription(mockThread);

        assertThat(groupDesc, containsString(uucpEmailAddress));   // using email address in lieu of name
        assertThat(groupDesc.length(), lessThanOrEqualTo(140));  // length is ok
        assertThat(groupDesc, endsWith("Z"));  // ends with the timestamp
    }

    @Test
    public void testMessageDisplayFormat() {
        testMessageSource.addMessage("displayheaderfrom.label", "From: {0}");
        testMessageSource.addMessage("displayheaderdate.label", "Date: {0}");
        testMessageSource.addMessage("displayheadersubject.label", "Subject: {0}");

        Message mockMessage = mock(Message.class);

        UserRecord sender = new UserRecord();
        sender.setName("Bob Loblaw");
        sender.setEmailAddress("bob@loblawlawfirm.com");
        when(mockMessage.getSender()).thenReturn(sender);

        ZonedDateTime sentDate = ZonedDateTime.parse("1666-01-01T00:00:00Z");
        when(mockMessage.getSentDate()).thenReturn(sentDate);

        when(mockMessage.getText()).thenReturn("Blah blah blah");

        when(mockMessage.getSubject()).thenReturn("Bobble awe");

        String formattedMessage = fmtr.formatMessageForDisplay(mockMessage);

        assertThat(formattedMessage, containsString(sender.getName()));
        assertThat(formattedMessage, containsString(sender.getEmailAddress()));
        assertThat(formattedMessage, containsString("" + sentDate.getYear()));
        assertThat(formattedMessage, containsString(mockMessage.getText()));
        assertThat(formattedMessage, containsString(mockMessage.getSubject()));
    }


    @Test
    public void testMessageDisplayFormatNoSenderName() {
        testMessageSource.addMessage("displayheaderfrom.label", "From: {0}");
        testMessageSource.addMessage("displayheaderdate.label", "Date: {0}");
        testMessageSource.addMessage("displayheadersubject.label", "Subject: {0}");

        Message mockMessage = mock(Message.class);

        UserRecord sender = new UserRecord();
        sender.setEmailAddress("bob@loblawlawfirm.com");
        when(mockMessage.getSender()).thenReturn(sender);

        ZonedDateTime sentDate = ZonedDateTime.parse("1666-01-01T00:00:00Z");
        when(mockMessage.getSentDate()).thenReturn(sentDate);

        when(mockMessage.getText()).thenReturn("Blah blah blah");

        when(mockMessage.getSubject()).thenReturn("Bobble awe");

        String formattedMessage = fmtr.formatMessageForDisplay(mockMessage);

        assertThat(formattedMessage, containsString(sender.getEmailAddress()));
        assertThat(formattedMessage, containsString("" + sentDate.getYear()));
        assertThat(formattedMessage, containsString(mockMessage.getText()));
        assertThat(formattedMessage, containsString(mockMessage.getSubject()));
    }


    // An alternative to mocking MessageSource.
    private static class TestMessageSource implements MessageSource {

        private Map<String, String> messageMap = new HashMap<>();

        public void addMessage(String key, String msg) {
            messageMap.put(key, msg);
        }

        @Override
        public String getMessage(String s, Object[] objects, String s1, Locale locale) {
            return getMessage(s, objects, locale);
        }

        @Override
        public String getMessage(String s, Object[] objects, Locale locale) throws NoSuchMessageException {
            String msg = messageMap.get(s);
            if (msg != null)
                return MessageFormat.format(msg, objects);
            else
                throw new NoSuchMessageException(s);
        }

        @Override
        public String getMessage(MessageSourceResolvable messageSourceResolvable, Locale locale) throws NoSuchMessageException {
            return null;
        }
    }
}
