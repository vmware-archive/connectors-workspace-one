/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("squid:S1075")
public final class ApiUrlConstants {
    public static final String INBOX_TASKS_API_PATH = "/inboxTasks";
    public static final String COMMUNITY_COMMON_API_V1 = "/api/v1/";
    public static final String BUSINESS_PROCESS_API_V1 = "/businessProcess/v1/";
    public static final String WORKERS_INBOX_TASKS_API = "/workers/me" + INBOX_TASKS_API_PATH;
    public static final String URL_PATH_SEPARATOR = "/";
    public static final String TIME_OFF_REQUEST_API_PATH = "/timeOffRequest/";
    public static final String APPROVE_EVENT_STEP_PATH = "/eventSteps/{ID}/approve";
    public static final String DECLINE_EVENT_STEP_PATH = "/eventSteps/{ID}/deny";

    public static final String INBOX_TASKS_VIEW_QUERY_PARAM_NAME = "view";
    public static final String INBOX_TASKS_SUMMARY = "inboxTaskSummary";
    public static final String TASK_ACTION_APPROVAL = "approval";
    public static final String TASK_ACTION_DENIAL = "denial";
    public static final String ACTION_TYPE_QUERY_PARAM = "type";
    public static final String INBOX_TASK_ID_PATH_VARIABLE = "id";

    private static final String API = "api";
    private static final String VERSION_1 = "/v1";
    public static final String CARDS_REQUESTS_API = "/cards/requests";
    public static final String TIMEOFF_TASKS_API = API + VERSION_1 + "/inbox-tasks/";
    private static final String TASK_ID_PATH_PARAM = "{id}";
    public static final String APPROVE_ACTION = "approve";
    public static final String DECLINE_ACTION = "decline";
    private static final String TIMEOFF_TASK_ACTION_RELATIVE_PATH = TIMEOFF_TASKS_API + TASK_ID_PATH_PARAM + URL_PATH_SEPARATOR;
    public static final String TIMEOFF_TASK_APPROVE_ACTION_API_PATH = TIMEOFF_TASK_ACTION_RELATIVE_PATH + APPROVE_ACTION;
    public static final String TIMEOFF_TASK_DECLINE_ACTION_API_PATH = TIMEOFF_TASK_ACTION_RELATIVE_PATH + DECLINE_ACTION;
}
