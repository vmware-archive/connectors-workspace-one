/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.utils;

import com.vmware.connectors.common.payloads.response.CardHeaderLinks;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CardConstants {
    public static final boolean PRIMARY_ACTION = true;
    public static final boolean SECONDARY_ACTION = false;
    public static final CardHeaderLinks NO_HEADER_LINKS = null;
    public static final int REASON_MIN_LENGTH = 1;

    public static final String COMMENT_KEY = "comment";
    public static final String REASON_KEY = "reason";
    public static final String TIMEOFF_APPROVE_BUTTON_LABEL_KEY = "timeoff.approve";
    public static final String TIMEOFF_APPROVE_COMMENT_LABEL_KEY = "timeoff.approve.comment";
    public static final String TIMEOFF_DECLINE_BUTTON_LABEL_KEY = "timeoff.decline";
    public static final String TIMEOFF_DECLINE_REASON_LABEL_KEY = "timeoff.decline.reason";
    public static final String CARD_HEADER_KEY = "card.header";
    public static final String TIMEOFF_REQUESTED_BY_KEY = "timeoff.requested.by";
    public static final String TIMEOFF_TOTAL_KEY = "timeoff.total";
    public static final String TIMEOFF_ASSIGNED_ON_KEY = "timeoff.assigned.on";
    public static final String TIMEOFF_START_DATE_KEY = "timeoff.start.date";
    public static final String TIMEOFF_END_DATE_KEY = "timeoff.end.date";
    public static final String TIMEOFF_ENTRY_TITLE_DAY_INDEXING_KEY = "timeoff.entry.title.day.indexing";
    public static final String TIMEOFF_ENTRY_TITLE_DETAILS_KEY = "timeoff.entry.title.details";
    public static final String TIMEOFF_ENTRY_DATE_KEY = "timeoff.entry.date";
    public static final String TIMEOFF_ENTRY_TYPE_KEY = "timeoff.entry.type";
    public static final String TIMEOFF_ENTRY_DAY_KEY = "timeoff.entry.day";
    public static final String TIMEOFF_ENTRY_TOTAL_KEY = "timeoff.entry.total";

    public static final String CARD_ACTION_INPUT_TEXTAREA = "textarea";

    public static final String TIMEOFF_TASK_ID = "timeoff_task_id";
    public static final String TIMEOFF_TASK_DESCRIPTOR = "timeoff_task_descriptor";
    public static final String TIMEOFF_TASK_URL = "timeoff_task_url";
    public static final String TIMEOFF_TASK_ACTION_COMMENTS = "timeoff_task_action_comments";
    public static final String TIMEOFF_TASK_DESCRIPTOR_VALUE = "Approval by Manager";

    public static final String DATE_FORMAT_MMMM_DD_YYYY = "MMMM, dd, yyyy";
    public static final List<String> NO_SUBTITLES = null;
    public static final String ONBOARDING_SETUP = "Onboarding Setup";
    public static final String TASK_TITLE = "task.title";
    public static final String TASK_NAME = "task.name";
    public static final String TASK_DUE = "task.due";
    public static final String OPEN_IN_WORKDAY = "Open In Workday";
    public static final String DAY0_CARDS_URI = "/cards/day0/requests";
}
