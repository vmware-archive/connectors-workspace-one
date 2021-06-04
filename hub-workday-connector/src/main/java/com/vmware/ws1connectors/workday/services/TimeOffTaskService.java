/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.google.common.collect.Maps;
import com.vmware.ws1connectors.workday.exceptions.TimeOffTaskActionException;
import com.vmware.ws1connectors.workday.exceptions.TimeOffTaskActionRequestCreationFailedException;
import com.vmware.ws1connectors.workday.exceptions.TimeOffTaskException;
import com.vmware.ws1connectors.workday.exceptions.TimeOffTaskNotFoundException;
import com.vmware.ws1connectors.workday.models.ApprovalTask;
import com.vmware.ws1connectors.workday.models.Descriptor;
import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffEvent;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.ACTION_TYPE_QUERY_PARAM;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.APPROVE_EVENT_STEP_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.BUSINESS_PROCESS_API_V1;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.DECLINE_EVENT_STEP_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TASK_ACTION_APPROVAL;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TASK_ACTION_DENIAL;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIME_OFF_REQUEST_API_PATH;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotBlank;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkBasicConnectorArgumentsNotBlank;
import static com.vmware.ws1connectors.workday.utils.CardConstants.BUSINESS_PROCESS_PATH;
import static com.vmware.ws1connectors.workday.utils.CardConstants.BUSINESS_TITLE_CHANGE_PATH;
import static com.vmware.ws1connectors.workday.utils.CardConstants.COMMENT;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_TASK_ACTION_COMMENTS;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_TASK_DESCRIPTOR;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_TASK_DESCRIPTOR_VALUE;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_TASK_ID;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_TASK_URL;
import static com.vmware.ws1connectors.workday.utils.TimeOffTaskUtils.createTimeOffTask;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.LOCALE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@Slf4j
public class TimeOffTaskService implements ApprovalTaskService {

    private static final String STATUS_AWAITING_ACTION = "Awaiting Action";
    private static final String INBOX_TASK_ID = "Inbox Task ID";

    @Autowired private WebClient restClient;
    @Autowired private InboxService inboxService;
    @Autowired private ApprovalTaskServiceFactory approvalTaskServiceFactory;
    @Value("classpath:static/templates/time_off_task_action_body_template.json")
    private Resource timeOffTaskActionTemplate;

    public Flux<ApprovalTask> getApprovalTasks(final String baseUrl, final String workdayAccessToken, final String tenantName, final Locale locale) {
        checkBasicConnectorArgumentsNotBlank(baseUrl, workdayAccessToken, tenantName);
        checkArgumentNotNull(locale, LOCALE);
        return inboxService.getTasks(baseUrl, workdayAccessToken, tenantName)
                .filter(this::isValidApprovalTask)
                .concatMap(inboxTask -> approvalTaskServiceFactory.getApprovalTaskService(inboxTask)
                        .map(approvalTaskService -> approvalTaskService
                                .getApprovalTaskDetails(baseUrl, workdayAccessToken, inboxTask, locale))
                        .orElseGet(Mono::empty)
                        .onErrorResume(throwable -> {
                            LOGGER.error("Error occurred while fetching details for ApprovalTask request {}",
                                    inboxTask.getOverallProcess().getId(), throwable);
                            return Mono.empty();
                        }));
    }

    private boolean isValidApprovalTask(InboxTask inboxTask) {
        if (inboxTask.getStatus() == null || inboxTask.getOverallProcess() == null) {
            return false;
        }
        final String statusDescriptor = inboxTask.getStatus().getDescriptor();
        final Descriptor overallProcess = inboxTask.getOverallProcess();
        LOGGER.info("Processing inbox task with id {} is for time off approval type: status-descriptor: {}, stepType-descriptor: {} and overallProcessHref: {}",
            inboxTask.getId(), statusDescriptor, inboxTask.getStepType(), overallProcess);
        return equalsIgnoreCase(statusDescriptor, STATUS_AWAITING_ACTION)
                && (endsWithIgnoreCase(overallProcess.getHref(),
                TIME_OFF_REQUEST_API_PATH + inboxTask.getOverallProcess().getId())
                || endsWithIgnoreCase(overallProcess.getHref(),
                BUSINESS_TITLE_CHANGE_PATH + inboxTask.getOverallProcess().getId())
                || endsWithIgnoreCase(overallProcess.getHref(),
                BUSINESS_PROCESS_PATH + inboxTask.getOverallProcess().getId()));
    }

    @Override public Mono<TimeOffTask> getApprovalTaskDetails(final String baseUrl, final String workdayAccessToken, final InboxTask inboxTask, final Locale locale) {
        LOGGER.info("Getting timeOff approval event corresponding to inbox task: inbox-id: {}, intent: {}, assigned: {}, user: {}, link: {}.",
            inboxTask.getId(), inboxTask.getDescriptor(), inboxTask.getAssigned(), inboxTask.getSubject().getDescriptor(), inboxTask.getHref());
        return getTimeOffEvents(workdayAccessToken, inboxTask)
            .filter(timeOffEvent -> !CollectionUtils.isEmpty(timeOffEvent.getTimeOffEntries()))
            .filter(timeOffEvent -> !CollectionUtils.isEmpty(timeOffEvent.getEventRecordsAwaitingAction()))
            .flatMap(timeOffEvent -> Mono.just(createTimeOffTask(inboxTask, timeOffEvent, locale)));
    }

    private Mono<TimeOffEvent> getTimeOffEvents(final String workdayAccessToken, final InboxTask inboxTask) {
        LOGGER.info("Getting event view of timeOff approval request: id: {}, intent: {}, assigned: {}, user: {}, link: {}",
            inboxTask.getId(), inboxTask.getDescriptor(), inboxTask.getAssigned(), inboxTask.getSubject().getDescriptor(), inboxTask.getHref());
        final String timeOffRequestUrl = inboxTask.getOverallProcess().getHref();
        LOGGER.info("Executing request to get event view of time off request url: {}", timeOffRequestUrl);
        return restClient.get()
            .uri(timeOffRequestUrl)
            .accept(APPLICATION_JSON)
            .header(AUTHORIZATION, workdayAccessToken)
            .retrieve()
            .onStatus(HttpStatus::isError, errorResponse -> {
                LOGGER.error("Resulted in error code {} for url {}", errorResponse.statusCode(), timeOffRequestUrl);
                return Mono.just(new TimeOffTaskException("Error occurred while retrieving time off task details for inbox task", errorResponse.statusCode()));
            })
            .bodyToMono(TimeOffEvent.class);
    }

    public Mono<Descriptor> approveTimeOffTask(final String baseUrl, final String workdayAccessToken,
                                               final String tenantName, final String inboxTaskId, final String comment) {
        return executeTimeOffTaskAction(baseUrl, workdayAccessToken, tenantName, inboxTaskId, TASK_ACTION_APPROVAL, comment);
    }

    public Mono<Descriptor> declineTimeOffTask(final String baseUrl, final String workdayAccessToken,
                                               final String email, final String inboxTaskId, final String reason) {
        return executeTimeOffTaskAction(baseUrl, workdayAccessToken, email, inboxTaskId, TASK_ACTION_DENIAL, reason);
    }

    private Mono<Descriptor> executeTimeOffTaskAction(final String baseUrl, final String workdayAccessToken,
                                                      final String tenantName, final String inboxTaskId,
                                                      final String action, final String comment) {
        checkBasicConnectorArgumentsNotBlank(baseUrl, workdayAccessToken, tenantName);
        checkArgumentNotBlank(inboxTaskId, INBOX_TASK_ID);
        return executeActionRequest(baseUrl, workdayAccessToken, inboxTaskId, action, comment, tenantName);
    }

    private Mono<Descriptor> executeActionRequest(final String baseUrl, final String workdayAccessToken, final String inboxTaskId,
                                                  final String action, final String comments, final String tenantName) {
        return inboxService.getTasks(baseUrl, workdayAccessToken, tenantName)
            .filter(inboxTask -> equalsIgnoreCase(inboxTask.getId(), inboxTaskId))
            .singleOrEmpty()
            .switchIfEmpty(Mono.error(() -> {
                LOGGER.error("Time off task {} not found", inboxTaskId);
                return new TimeOffTaskNotFoundException(inboxTaskId);
            }))
            .flatMap(inboxTask -> executeAction(baseUrl, workdayAccessToken, inboxTask, action, comments, tenantName));
    }

    private Mono<Descriptor> executeAction(final String baseUrl, final String workdayAccessToken, final InboxTask inboxTask,
                                           final String action, final String comments, String tenantName) {
        String inboxTaskId = inboxTask.getId();
        if (inboxTask.getOverallProcess().getHref().contains(BUSINESS_PROCESS_PATH)) {
            return executeBusinessProcessAction(baseUrl, workdayAccessToken, action, comments, inboxTaskId, tenantName);
        }
        final String requestBody = getTaskActionRequestBody(inboxTaskId, inboxTask.getHref(), comments);
        final String inboxTaskActionUrl = getTaskActionUrl(inboxTask.getHref(), action);
        LOGGER.info("Executing request url: {}, request body: {}.", inboxTaskActionUrl, requestBody);
        return restClient.put()
            .uri(inboxTaskActionUrl)
            .header(HttpHeaders.AUTHORIZATION, workdayAccessToken)
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(HttpStatus::isError, errorResponse -> {
                LOGGER.info("Response status code after executing action: {} on inbox task with id: {} is {}.",
                    action, inboxTaskId, errorResponse.rawStatusCode());
                return Mono.error(new TimeOffTaskActionException("Error occurred in executing action for inbox task id: " + inboxTaskId, errorResponse.statusCode()));
            })
            .bodyToMono(Descriptor.class);
    }

    private Mono<Descriptor> executeBusinessProcessAction(String baseUrl, String workdayAccessToken, String action,
                                                          String comments, String inboxTaskId, String tenantName) {
        LOGGER.debug("Executing Approve/Deny Request for Business Process inboxTaskId: {}.", inboxTaskId);
        final URI businessProcessActionUrl = buildBusinessProcessActionUrl(baseUrl, inboxTaskId, action, tenantName);
        LOGGER.info("Executing Approve/Deny Request for Business Process with URL: {}.", businessProcessActionUrl);

        return restClient.post()
                .uri(businessProcessActionUrl)
                .header(HttpHeaders.AUTHORIZATION, workdayAccessToken)
                .contentType(APPLICATION_JSON)
                .bodyValue(singletonMap(COMMENT, comments))
                .retrieve()
                .onStatus(HttpStatus::isError, errorResponse -> {
                    LOGGER.error("Response status code after executing action: {} on inbox task with id: {} is {}.",
                            action, inboxTaskId, errorResponse.rawStatusCode());
                    return Mono.error(new TimeOffTaskActionException("Error occurred in executing action for inbox task id: " + inboxTaskId, errorResponse.statusCode()));
                })
                .bodyToMono(Descriptor.class);
    }

    private URI buildBusinessProcessActionUrl(String baseUrl, String inboxTaskId, String action, String tenantName) {
        String path;
        if (TASK_ACTION_APPROVAL.equals(action)) {
            path = BUSINESS_PROCESS_API_V1 + tenantName + APPROVE_EVENT_STEP_PATH;
        } else {
            path = BUSINESS_PROCESS_API_V1 + tenantName + DECLINE_EVENT_STEP_PATH;
        }
        return UriComponentsBuilder.fromHttpUrl(baseUrl).path(path).build(Map.of("ID", inboxTaskId));
    }

    private String getTaskActionUrl(final String inboxTaskUrl, final String action) {
        return UriComponentsBuilder.fromHttpUrl(inboxTaskUrl)
            .queryParam(ACTION_TYPE_QUERY_PARAM, Collections.singletonList(action))
            .build()
            .toUriString();
    }

    private String getTaskActionRequestBody(final String inboxTaskId, final String actionUrl, final String comments) {
        try (InputStream stream = timeOffTaskActionTemplate.getInputStream()) {
            final Map<String, String> paramMap = Maps.newHashMap();
            paramMap.put(TIMEOFF_TASK_ID, inboxTaskId);
            paramMap.put(TIMEOFF_TASK_DESCRIPTOR, TIMEOFF_TASK_DESCRIPTOR_VALUE);
            paramMap.put(TIMEOFF_TASK_URL, actionUrl);
            paramMap.put(TIMEOFF_TASK_ACTION_COMMENTS, HtmlUtils.htmlEscape(defaultIfBlank(comments, EMPTY)));
            final StringSubstitutor stringSubstitutor = new StringSubstitutor(paramMap);
            return stringSubstitutor.replace(IOUtils.toString(stream, UTF_8));
        } catch (IOException e) {
            LOGGER.error("Error occurred in building inbox task action body from template", e);
            throw new TimeOffTaskActionRequestCreationFailedException(e);
        }
    }

}
