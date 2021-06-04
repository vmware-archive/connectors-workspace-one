/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.controllers;

import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.domain.BotAction;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.domain.BotObjects;
import com.vmware.ws1connectors.servicenow.domain.snow.Task;
import com.vmware.ws1connectors.servicenow.domain.snow.TaskResults;
import com.vmware.ws1connectors.servicenow.forms.CreateTaskForm;
import com.vmware.ws1connectors.servicenow.forms.ViewTaskForm;
import com.vmware.ws1connectors.servicenow.utils.BotActionBuilder;
import com.vmware.ws1connectors.servicenow.utils.BotObjectBuilderUtils;
import com.vmware.ws1connectors.servicenow.utils.TabularDataBuilderUtils;
import com.vmware.ws1connectors.servicenow.utils.UriBuilderUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.AUTH_HEADER;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.BASE_URL_HEADER;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.CALLER_ID;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.CONFIRM_CREATE_TASK_URL;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.CREATE_TASK_URL;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.GET_TASKS_URL;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.INSERT_OBJECT_TYPE;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.ITEM_DETAILS;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.JSON_PATH_RESULT_NUMBER;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.MAX_NO_OF_RECENT_TICKETS_TO_FETCH;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.NO_OPEN_TICKETS_MSG;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.NUMBER;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.OBJECTS;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.OBJECT_TYPE_BOT_DISCOVERY;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.OBJECT_TYPE_TASK;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.ROUTING_PREFIX_TEMPLATE;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SHORT_UNDERSCORE_DESCRIPTION;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SNOW_SYS_PARAM_ACTIVE;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SNOW_SYS_PARAM_DISPLAY_VALUE;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SNOW_SYS_PARAM_LIMIT;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SNOW_SYS_PARAM_OFFSET;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SNOW_SYS_PARAM_OPENED_BY_EMAIL;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SNOW_SYS_PARAM_QUERY;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SNOW_SYS_VALUE_ORDER_DESC_CREATED;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SNOW_TABLE_PATH;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_CONFIRMATION;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_STATUS;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_TEXT;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.VIEW_TASK_MSG_PROPS;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.VIEW_TASK;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
@Slf4j
public class TaskController extends BaseController {

    private final WebClient rest;
    private final BotActionBuilder botActionBuilder;
    private final ConnectorTextAccessor connectorTextAccessor;

    private static final String CREATE_CONFIRMATION = "createTask.confirmationRequest";

    @Autowired public TaskController(WebClient webClient, ConnectorTextAccessor connectorTextAccessor) {
        super();

        this.rest = webClient;
        this.botActionBuilder = new BotActionBuilder(connectorTextAccessor);
        this.connectorTextAccessor = connectorTextAccessor;
    }

    @PostMapping(
            path = GET_TASKS_URL,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<Map<String, List<Map<String, BotItem>>>> getTasks(
            @RequestHeader(AUTHORIZATION) String mfToken,
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            ViewTaskForm form,
            Locale locale) {

        String taskType = form.getType();
        String taskNumber = form.getNumber();
        int ticketsLimit = MAX_NO_OF_RECENT_TICKETS_TO_FETCH;

        var userEmail = AuthUtil.extractUserEmail(mfToken);

        LOGGER.trace("getTasks for type={}, baseUrl={}, userEmail={}, ticketsLimit={}",
                taskType, baseUrl, userEmail, ticketsLimit);

        URI taskUri;
        if (StringUtils.isBlank(taskNumber)) {
            taskUri = buildTaskUriReadUserTickets(baseUrl, taskType, ticketsLimit, userEmail);

            return retrieveTasks(taskUri, auth)
                    .map(taskList -> toViewTaskBotObj(taskList, baseUrl, locale));
        }
        taskUri = buildTaskUriReadByNumber(taskType, taskNumber, baseUrl);
        return retrieveTasks(taskUri, auth)
                .map(taskList -> toViewTaskByNumberBotObj(taskList, baseUrl, UI_TYPE_STATUS, locale));
    }

    private URI buildTaskUriReadUserTickets(@RequestHeader(BASE_URL_HEADER) String baseUrl, String taskType, int ticketsLimit, String userEmail) {
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .replacePath(SNOW_TABLE_PATH)
                .path(taskType)
                .queryParam(SNOW_SYS_PARAM_DISPLAY_VALUE, Boolean.TRUE) // We want to show label, not code.
                .queryParam(SNOW_SYS_PARAM_LIMIT, ticketsLimit)
                .queryParam(SNOW_SYS_PARAM_OFFSET, 0)
                .queryParam(SNOW_SYS_PARAM_OPENED_BY_EMAIL, userEmail)
                .queryParam(SNOW_SYS_PARAM_ACTIVE, Boolean.TRUE)   // Ignore already closed tickets.
                .queryParam(SNOW_SYS_PARAM_QUERY, SNOW_SYS_VALUE_ORDER_DESC_CREATED) // Order by latest created tickets.
                .encode().build().toUri();
    }

    @PostMapping(
            path = CONFIRM_CREATE_TASK_URL,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BotObjects confirmCreateTask(
            @RequestHeader(AUTHORIZATION) String mfToken,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX_TEMPLATE) String routingPrefixTemplate,
            Locale locale,
            @Valid CreateTaskForm form) {
        String taskType = form.getType();
        String shortDescription = form.getShortDescription();

        var userEmail = AuthUtil.extractUserEmail(mfToken);
        String routingPrefix = routingPrefixTemplate.replace(INSERT_OBJECT_TYPE, OBJECT_TYPE_BOT_DISCOVERY);

        LOGGER.trace("confirm createTicket for baseUrl={}, taskType={}, userEmail={}", baseUrl, taskType, userEmail);
        BotAction confirmAction = botActionBuilder.confirmTaskCreate(shortDescription, routingPrefix, locale, CREATE_TASK_URL, taskType);

        return BotObjectBuilderUtils.confirmationObject(connectorTextAccessor, botActionBuilder, CREATE_CONFIRMATION, routingPrefix, locale, confirmAction);
    }

    @PostMapping(
            path = CREATE_TASK_URL,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<Map<String, List<Map<String, BotItem>>>> createTask(
            @RequestHeader(AUTHORIZATION) String mfToken,
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            Locale locale,
            @Valid CreateTaskForm form) {
        String taskType = form.getType();
        String shortDescription = form.getShortDescription();

        var userEmail = AuthUtil.extractUserEmail(mfToken);

        LOGGER.trace("createTicket for baseUrl={}, taskType={}, userEmail={}", baseUrl, taskType, userEmail);

        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return this.createTask(taskType, shortDescription, userEmail, baseUri, auth)
                .map(taskNumber -> buildTaskUriReadByNumber(taskType, taskNumber, baseUrl))
                .flatMap(readTaskUri -> retrieveTasks(readTaskUri, auth))
                .map(retrieved -> toViewTaskByNumberBotObj(retrieved, baseUrl, UI_TYPE_CONFIRMATION, locale));
    }

    private URI buildTaskUriReadByNumber(String taskType, String taskNumber, String baseUrl) {
        // If task number is provided, maybe it doesn't matter to apply the filter about who created the ticket.
        // Also, we will show the ticket even if its closed.
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .replacePath(SNOW_TABLE_PATH)
                .path(taskType)
                .queryParam(SNOW_SYS_PARAM_DISPLAY_VALUE, Boolean.TRUE)
                .queryParam(NUMBER, taskNumber)
                .encode().build().toUri();
    }

    private Mono<List<Task>> retrieveTasks(URI taskUri, String auth) {
        return rest.get()
                .uri(taskUri)
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(TaskResults.class)
                .map(TaskResults::getResult);
    }

    private Map<String, List<Map<String, BotItem>>> toViewTaskByNumberBotObj(List<Task> tasks, String baseUrl,
                                                                             String uiType, Locale locale) {
        if (tasks.isEmpty()) {
            return Map.of(OBJECTS, buildBotObjectsWhenTasksNotAvailable(baseUrl, locale));
        }
        List<Map<String, BotItem>> taskObjects = new ArrayList<>();
        buildTaskObjects(tasks, baseUrl, uiType, locale, taskObjects);
        return Map.of(OBJECTS, taskObjects);
    }

    private List<Map<String, BotItem>> buildBotObjectsWhenTasksNotAvailable(String baseUrl, Locale locale) {
        return Collections.singletonList(
                Map.of(ITEM_DETAILS,
                        new BotItem.Builder()
                                .setTitle(
                                        connectorTextAccessor.getTitle(NO_OPEN_TICKETS_MSG, locale))
                                .setDescription(connectorTextAccessor
                                        .getDescription(NO_OPEN_TICKETS_MSG, locale))
                                .setUrl(new Link(baseUrl))
                                .setType(UI_TYPE_STATUS)
                                .setWorkflowStep(WorkflowStep.COMPLETE)
                                .build()));
    }

    private Map<String, List<Map<String, BotItem>>> toViewTaskBotObj(List<Task> tasks, String baseUrl,
                                                                     Locale locale) {
        if (tasks.isEmpty()) {
            return Map.of(OBJECTS, buildBotObjectsWhenTasksNotAvailable(baseUrl, locale));
        }
        List<Map<String, BotItem>> taskObjects = new ArrayList<>();
        taskObjects.add(Map.of(ITEM_DETAILS,
                new BotItem.Builder()
                        .setTitle(connectorTextAccessor.getTitle(VIEW_TASK, locale))
                        .setType(UI_TYPE_TEXT)
                        .setWorkflowStep(WorkflowStep.COMPLETE)
                        .build()));
        buildTaskObjects(tasks, baseUrl, UI_TYPE_STATUS, locale, taskObjects);
        return Map.of(OBJECTS, taskObjects);
    }

    private void buildTaskObjects(List<Task> tasks, String baseUrl, String uiType, Locale locale,
                                  List<Map<String, BotItem>> taskObjects) {
        tasks.forEach(task ->
                taskObjects.add(Map.of(ITEM_DETAILS,
                        new BotItem.Builder()
                                .setTitle(connectorTextAccessor
                                        .getTitle(OBJECT_TYPE_TASK, locale, task.getNumber()))
                                .setSubtitle(task.getState())
                                .setDescription(task.getShortDescription())
                                .setUrl(UriBuilderUtils.getTaskUrl(baseUrl, task))
                                .setType(uiType)
                                .addTabularData(TabularDataBuilderUtils.buildTabularDataForTask(task, baseUrl))
                                .setWorkflowStep(WorkflowStep.COMPLETE)
                                .build()))
        );
        if (tasks.size() == MAX_NO_OF_RECENT_TICKETS_TO_FETCH) {
            taskObjects.add(Map.of(ITEM_DETAILS,
                    new BotItem.Builder()
                            .setTitle(connectorTextAccessor.getMessage(VIEW_TASK_MSG_PROPS, locale, baseUrl))
                            .setType(UI_TYPE_TEXT)
                            .setWorkflowStep(WorkflowStep.COMPLETE)
                            .build()));
        }
    }

    private Mono<String> createTask(String taskType, String shortDescription, String callerEmailId,
                                    URI baseUri, String auth) {
        return rest.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path(SNOW_TABLE_PATH)
                        .path(taskType)
                        .build()
                )
                .header(AUTHORIZATION, auth)
                // ToDo: Improve this request body, if somehow chat-bot is able to supply more info.
                .bodyValue(Map.of(
                        SHORT_UNDERSCORE_DESCRIPTION, shortDescription,
                        CALLER_ID, callerEmailId))
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .map(doc -> doc.read(JSON_PATH_RESULT_NUMBER));
    }
}
