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
import com.vmware.ws1connectors.workday.exceptions.UserNotFoundException;
import com.vmware.ws1connectors.workday.models.Descriptor;
import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.models.WorkdayUser;
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
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.ACTION_TYPE_QUERY_PARAM;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASKS_API_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TASK_ACTION_APPROVAL;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TASK_ACTION_DENIAL;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIME_OFF_REQUEST_API_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.URL_PATH_SEPARATOR;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotBlank;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkBasicConnectorArgumentsNotBlank;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_TASK_ACTION_COMMENTS;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_TASK_DESCRIPTOR;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_TASK_DESCRIPTOR_VALUE;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_TASK_ID;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TIMEOFF_TASK_URL;
import static com.vmware.ws1connectors.workday.utils.TimeOffTaskUtils.createTimeOffTask;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.LOCALE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@Slf4j
public class TimeOffTaskService {

    private static final String STATUS_AWAITING_ACTION = "Awaiting Action";
    private static final String INBOX_TASK_ID = "Inbox Task ID";

    @Autowired private WebClient restClient;
    @Autowired private UserService userService;
    @Autowired private InboxService inboxService;
    @Value("classpath:static/templates/time_off_task_action_body_template.json")
    private Resource timeOffTaskActionTemplate;

    public Flux<TimeOffTask> getTimeOffTasks(final String baseUrl, final String workdayAccessToken, final String userEmail, final Locale locale) {
        checkBasicConnectorArgumentsNotBlank(baseUrl, workdayAccessToken, userEmail);
        checkArgumentNotNull(locale, LOCALE);
        return userService.getUser(baseUrl, workdayAccessToken, userEmail)
            .switchIfEmpty(Mono.error(UserNotFoundException::new))
            .flatMapMany(user -> inboxService.getTasks(baseUrl, workdayAccessToken, user))
            .filter(this::isTimeOffTask)
            .concatMap(inboxTask -> getTimeOffTaskDetails(baseUrl, workdayAccessToken, inboxTask, locale)
                .onErrorResume(throwable -> {
                    LOGGER.error("Error occurred while fetching details for time off request {}", inboxTask.getOverallProcess().getId(), throwable);
                    return Mono.empty();
                }));
    }

    private boolean isTimeOffTask(InboxTask inboxTask) {
        final String statusDescriptor = inboxTask.getStatus().getDescriptor();
        final String stepTypeDescriptor = inboxTask.getStepType().getDescriptor();
        final Descriptor overallProcess = inboxTask.getOverallProcess();
        LOGGER.info("Processing inbox task with id {} is for time off approval type: status-descriptor: {}, stepType-descriptor: {} and overallProcessHref: {}",
            inboxTask.getId(), statusDescriptor, stepTypeDescriptor, overallProcess.getHref());
        return equalsIgnoreCase(statusDescriptor, STATUS_AWAITING_ACTION)
            && endsWithIgnoreCase(overallProcess.getHref(), TIME_OFF_REQUEST_API_PATH + inboxTask.getOverallProcess().getId());
    }

    private Mono<TimeOffTask> getTimeOffTaskDetails(final String baseUrl, final String workdayAccessToken, final InboxTask inboxTask, final Locale locale) {
        LOGGER.info("Getting timeOff approval event corresponding to inbox task: inbox-id: {}, intent: {}, assigned: {}, user: {}, link: {}.",
            inboxTask.getId(), inboxTask.getDescriptor(), inboxTask.getAssigned(), inboxTask.getSubject().getDescriptor(), inboxTask.getHref());
        return getTimeOffEvents(baseUrl, workdayAccessToken, inboxTask)
            .filter(timeOffEvent -> !CollectionUtils.isEmpty(timeOffEvent.getTimeOffEntries()))
            .filter(timeOffEvent -> !CollectionUtils.isEmpty(timeOffEvent.getEventRecordsAwaitingAction()))
            .flatMap(timeOffEvent -> Mono.just(createTimeOffTask(inboxTask, timeOffEvent, locale)));
    }

    private Mono<TimeOffEvent> getTimeOffEvents(final String baseUrl, final String workdayAccessToken, final InboxTask inboxTask) {
        LOGGER.info("Getting event view of timeOff approval request: id: {}, intent: {}, assigned: {}, user: {}, link: {}",
            inboxTask.getId(), inboxTask.getDescriptor(), inboxTask.getAssigned(), inboxTask.getSubject().getDescriptor(), inboxTask.getHref());
        final String timeOffRequestUrl = getTimeOffRequestUrl(baseUrl, inboxTask);
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

    private String getTimeOffRequestUrl(String baseUrl, InboxTask inboxTask) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path(TIME_OFF_REQUEST_API_PATH)
            .path(inboxTask.getOverallProcess().getId())
            .build()
            .toUriString();
    }

    public Mono<Descriptor> approveTimeOffTask(final String baseUrl, final String workdayAccessToken,
                                               final String email, final String inboxTaskId, final String comment) {
        return executeTimeOffTaskAction(baseUrl, workdayAccessToken, email, inboxTaskId, TASK_ACTION_APPROVAL, comment);
    }

    public Mono<Descriptor> declineTimeOffTask(final String baseUrl, final String workdayAccessToken,
                                               final String email, final String inboxTaskId, final String reason) {
        return executeTimeOffTaskAction(baseUrl, workdayAccessToken, email, inboxTaskId, TASK_ACTION_DENIAL, reason);
    }

    private Mono<Descriptor> executeTimeOffTaskAction(final String baseUrl, final String workdayAccessToken,
                                                      final String userEmail, final String inboxTaskId,
                                                      final String action, final String comment) {
        checkBasicConnectorArgumentsNotBlank(baseUrl, workdayAccessToken, userEmail);
        checkArgumentNotBlank(inboxTaskId, INBOX_TASK_ID);
        return userService.getUser(baseUrl, workdayAccessToken, userEmail)
            .switchIfEmpty(Mono.error(UserNotFoundException::new))
            .flatMap(user -> executeActionRequest(baseUrl, workdayAccessToken, inboxTaskId, action, comment, user));
    }

    private Mono<Descriptor> executeActionRequest(final String baseUrl, final String workdayAccessToken, final String inboxTaskId,
                                                  final String action, final String comments, final WorkdayUser user) {
        return inboxService.getTasks(baseUrl, workdayAccessToken, user)
            .filter(inboxTask -> equalsIgnoreCase(inboxTask.getId(), inboxTaskId))
            .singleOrEmpty()
            .switchIfEmpty(Mono.error(() -> {
                LOGGER.error("Time off task {} not found", inboxTaskId);
                return new TimeOffTaskNotFoundException(inboxTaskId);
            }))
            .flatMap(inboxTask -> executeAction(baseUrl, workdayAccessToken, inboxTaskId, action, comments));
    }

    private Mono<Descriptor> executeAction(final String baseUrl, final String workdayAccessToken, final String inboxTaskId,
                                           final String action, final String comments) {
        final String inboxTaskUrl = getInboxTaskUrl(baseUrl, inboxTaskId);
        final String requestBody = getTaskActionRequestBody(inboxTaskId, inboxTaskUrl, comments);
        final String inboxTaskActionUrl = getTaskActionUrl(inboxTaskUrl, action);
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

    private String getTaskActionUrl(final String inboxTaskUrl, final String action) {
        return UriComponentsBuilder.fromHttpUrl(inboxTaskUrl)
            .queryParam(ACTION_TYPE_QUERY_PARAM, Collections.singletonList(action))
            .build()
            .toUriString();
    }

    private String getInboxTaskUrl(final String baseUrl, final String inboxTaskId) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path(INBOX_TASKS_API_PATH)
            .path(URL_PATH_SEPARATOR)
            .path(inboxTaskId)
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
