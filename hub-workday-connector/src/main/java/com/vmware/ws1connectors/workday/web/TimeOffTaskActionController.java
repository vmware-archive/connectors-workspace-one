/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.web;

import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.ws1connectors.workday.forms.ApproveForm;
import com.vmware.ws1connectors.workday.forms.DeclineForm;
import com.vmware.ws1connectors.workday.services.TimeOffTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASK_ID_PATH_VARIABLE;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIMEOFF_TASK_APPROVE_ACTION_API_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIMEOFF_TASK_DECLINE_ACTION_API_PATH;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.BASE_URL_HEADER;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.CONNECTOR_AUTH_HEADER;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Slf4j
public class TimeOffTaskActionController {
    @Autowired private TimeOffTaskService timeOffTaskService;

    @PostMapping(path = TIMEOFF_TASK_APPROVE_ACTION_API_PATH, consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> approveTask(@PathVariable(INBOX_TASK_ID_PATH_VARIABLE) String inboxTaskId,
                                                    @RequestHeader(AUTHORIZATION) String authorization,
                                                    @RequestHeader(BASE_URL_HEADER) String baseUrl,
                                                    @RequestHeader(CONNECTOR_AUTH_HEADER) String connectorAuth,
                                                    @Valid ApproveForm form) {
        final String userEmail = AuthUtil.extractUserEmail(authorization);
        return timeOffTaskService.approveTimeOffTask(baseUrl, connectorAuth, userEmail, inboxTaskId, form.getComment())
            .map(descriptor -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    @PostMapping(path = TIMEOFF_TASK_DECLINE_ACTION_API_PATH, consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> declineTask(@PathVariable(INBOX_TASK_ID_PATH_VARIABLE) String inboxTaskId,
                                                    @RequestHeader(AUTHORIZATION) String authorization,
                                                    @RequestHeader(BASE_URL_HEADER) String baseUrl,
                                                    @RequestHeader(CONNECTOR_AUTH_HEADER) String connectorAuth,
                                                    @Valid DeclineForm form) {
        final String userEmail = AuthUtil.extractUserEmail(authorization);
        return timeOffTaskService.declineTimeOffTask(baseUrl, connectorAuth, userEmail, inboxTaskId, form.getReason())
            .map(descriptor -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

}
