/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.controllers;

import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.ws1connectors.servicenow.bot.discovery.capabilities.BotCapability;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.constants.WorkflowId;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.domain.snow.Task;
import com.vmware.ws1connectors.servicenow.domain.snow.TaskResults;
import com.vmware.ws1connectors.servicenow.forms.CreateTaskForm;
import com.vmware.ws1connectors.servicenow.forms.ViewTaskForm;
import com.vmware.ws1connectors.servicenow.utils.BotTextAccessor;
import com.vmware.ws1connectors.servicenow.utils.TabularDataBuilderUtils;
import com.vmware.ws1connectors.servicenow.utils.UriBuilderUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.OBJECTS;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_STATUS;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_TEXT;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.VIEW_TASK_MSG_PROPS;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.VIEW_TASK_TITLE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
@Slf4j
public class SNowBotController extends BaseController {

    @Autowired WebClient rest;
    @Autowired BotTextAccessor botTextAccessor;
    @Autowired ServerProperties serverProperties;

    @PostMapping(
            path = "/api/v1/tasks",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<Map<String, List<Map<String, BotItem>>>> getTasks(
            @RequestHeader(AUTHORIZATION) String mfToken,
            @RequestHeader(ServiceNowConstants.AUTH_HEADER) String auth,
            @RequestHeader(ServiceNowConstants.BASE_URL_HEADER) String baseUrl,
            ViewTaskForm form,
            Locale locale) {
        // User asks something like "What are my open tickets ?"

        // ToDo: If a customer doesn't call their parent ticket as "task", this flow would break.
        // A potential solution is to let admin configure the parent task name.

        String taskType = "task";
        String taskNumber = form.getNumber();

        // ToDo: Technically there can be too many open tickets. How many of them will be displayed in the bot UI ?
        int ticketsLimit = ServiceNowConstants.MAX_NO_OF_RECENT_TICKETS_TO_FETCH;

        var userEmail = AuthUtil.extractUserEmail(mfToken);

        LOGGER.trace("getTasks for type={}, baseUrl={}, userEmail={}, ticketsLimit={}",
                taskType, baseUrl, userEmail, ticketsLimit);

        URI taskUri;
        if (StringUtils.isBlank(taskNumber)) {
            taskUri = buildTaskUriReadUserTickets(taskType, userEmail, baseUrl, ticketsLimit);
            return retrieveTasks(taskUri, auth)
                    .map(taskList -> toViewTaskBotObj(taskList, baseUrl, UI_TYPE_STATUS, locale));
        }
        taskUri = buildTaskUriReadByNumber(taskType, taskNumber, baseUrl);
        return retrieveTasks(taskUri, auth)
                .map(taskList -> toViewTaskByNumberBotObj(taskList, baseUrl, UI_TYPE_STATUS, locale));
    }

    private URI buildTaskUriReadByNumber(String taskType, String taskNumber, String baseUrl) {
        // If task number is provided, maybe it doesn't matter to apply the filter about who created the ticket.
        // Also, we will show the ticket even if its closed.
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .replacePath("/api/now/table/")
                .path(taskType)
                .queryParam("sysparm_display_value", Boolean.TRUE)
                .queryParam(ServiceNowConstants.NUMBER, taskNumber)
                .encode().build().toUri();
    }

    private URI buildTaskUriReadUserTickets(String taskType, String userEmail, String baseUrl, int limit) {
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .replacePath("/api/now/table/")
                .path(taskType)
                .queryParam("sysparm_display_value", Boolean.TRUE) // We want to show label, not code.
                .queryParam(ServiceNowConstants.SNOW_SYS_PARAM_LIMIT, limit)
                .queryParam(ServiceNowConstants.SNOW_SYS_PARAM_OFFSET, 0)
                .queryParam("opened_by.email", userEmail)
                .queryParam("active", Boolean.TRUE)   // Ignore already closed tickets.
                .queryParam("sysparm_query", "ORDERBYDESCsys_created_on") // Order by latest created tickets.
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
                Map.of(ServiceNowConstants.ITEM_DETAILS,
                        new BotItem.Builder()
                                .setTitle(
                                        botTextAccessor.getObjectTitle(ServiceNowConstants.NO_OPEN_TICKETS_MSG, locale))
                                .setDescription(botTextAccessor
                                        .getActionDescription(ServiceNowConstants.NO_OPEN_TICKETS_MSG, locale))
                                .setUrl(new Link(baseUrl))
                                .setType(UI_TYPE_STATUS)
                                .setWorkflowStep(WorkflowStep.COMPLETE)
                                .build()));
    }

    private Map<String, List<Map<String, BotItem>>> toViewTaskBotObj(List<Task> tasks, String baseUrl,
                                                                 String uiType, Locale locale) {
        if (tasks.isEmpty()) {
            return Map.of(OBJECTS, buildBotObjectsWhenTasksNotAvailable(baseUrl, locale));
        }
        List<Map<String, BotItem>> taskObjects = new ArrayList<>();
        taskObjects.add(Map.of(ServiceNowConstants.ITEM_DETAILS,
                new BotItem.Builder()
                        .setTitle(botTextAccessor.getMessage(VIEW_TASK_TITLE, locale))
                        .setType(UI_TYPE_TEXT)
                        .setWorkflowStep(WorkflowStep.COMPLETE)
                        .build()));
        buildTaskObjects(tasks, baseUrl, uiType, locale, taskObjects);
        return Map.of(OBJECTS, taskObjects);
    }

    private void buildTaskObjects(List<Task> tasks, String baseUrl, String uiType, Locale locale,
                                  List<Map<String, BotItem>> taskObjects) {
        tasks.forEach(task ->
                taskObjects.add(Map.of(ServiceNowConstants.ITEM_DETAILS,
                        new BotItem.Builder()
                                .setTitle(botTextAccessor
                                        .getObjectTitle(ServiceNowConstants.OBJECT_TYPE_TASK, locale, task.getNumber()))
                                .setSubtitle(task.getState())
                                .setDescription(task.getShortDescription())
                                .setUrl(UriBuilderUtils.getTaskUrl(baseUrl, task))
                                .setType(uiType)
                                .addTabularData(TabularDataBuilderUtils.buildTabularDataForTask(task, baseUrl))
                                .setWorkflowStep(WorkflowStep.COMPLETE)
                                .build()))
        );
        if (tasks.size() == ServiceNowConstants.MAX_NO_OF_RECENT_TICKETS_TO_FETCH) {
            taskObjects.add(Map.of(ServiceNowConstants.ITEM_DETAILS,
                    new BotItem.Builder()
                            .setTitle(botTextAccessor.getMessage(VIEW_TASK_MSG_PROPS, locale, baseUrl))
                            .setType(UI_TYPE_TEXT)
                            .setWorkflowStep(WorkflowStep.COMPLETE)
                            .build()));
        }
    }

    // An object, 'botDiscovery', advertises all the capabilities of this connector, for bot use cases.
    // ToDo: After 1 flow works en-end, advertise remaining capabilities as well.
    @PostMapping(
            path = "/bot-discovery",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> getBotDiscovery(
            @RequestHeader(ServiceNowConstants.BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ServiceNowConstants.ROUTING_PREFIX) String routingPrefix,
            @Valid @RequestBody final CardRequest cardRequest,
            Locale locale
    ) {
        Map<String, String> connConfig = cardRequest.getConfig();
        String taskType = connConfig.get(ServiceNowConstants.CONFIG_FILE_TICKET_TABLE_NAME);
        LOGGER.trace("getBotDiscovery object. baseUrl: {}, routingPrefix: {}, fileTaskType: {}", baseUrl, routingPrefix, taskType);

        if (StringUtils.isBlank(taskType)) {
            LOGGER.debug("Table name isn't specified for ticket filing flow. Taking `task` as default type.");
            taskType = "task"; // ToDo: Not required after APF-2570.
        }

        return ResponseEntity.ok(
                buildBotDiscovery(taskType, routingPrefix, locale)
        );
    }

    private Map<String, Object> buildBotDiscovery(String taskType, String routingPrefix, Locale locale) {
        final String appContextPath = serverProperties.getServlet().getContextPath();
        return Map.of(OBJECTS, List.of(
                Map.of(
                        ServiceNowConstants.CHILDREN,
                        Arrays.stream(WorkflowId.values()).map(workflowIdEnum -> Map.of(ServiceNowConstants.ITEM_DETAILS,
                                BotCapability.build(workflowIdEnum, botTextAccessor, appContextPath)
                                        .describe(taskType, routingPrefix, locale)))

                )
                )
        );
    }

    @PostMapping(
            path = "/api/v1/task/create",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<Map<String, List<Map<String, BotItem>>>> createTask(
            @RequestHeader(AUTHORIZATION) String mfToken,
            @RequestHeader(ServiceNowConstants.AUTH_HEADER) String auth,
            @RequestHeader(ServiceNowConstants.BASE_URL_HEADER) String baseUrl,
            Locale locale,
            @Valid CreateTaskForm form) {

        // ToDo: Validate.
        // ToDo: Should we make it optional and default it to "task" ?
        String taskType = form.getType();

        String shortDescription = form.getShortDescription();

        var userEmail = AuthUtil.extractUserEmail(mfToken);

        LOGGER.trace("createTicket for baseUrl={}, taskType={}, userEmail={}", baseUrl, taskType, userEmail);

        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return this.createTask(taskType, shortDescription, userEmail, baseUri, auth)
                .map(taskNumber -> buildTaskUriReadByNumber(taskType, taskNumber, baseUrl))
                .flatMap(readTaskUri -> retrieveTasks(readTaskUri, auth))
                .map(retrieved -> toViewTaskByNumberBotObj(retrieved, baseUrl, "confirmation", locale));
    }

    private Mono<String> createTask(String taskType, String shortDescription, String callerEmailId,
                                    URI baseUri, String auth) {
        return rest.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path("/api/now/table/")
                        .path(taskType)
                        .build()
                )
                .header(AUTHORIZATION, auth)
                // ToDo: Improve this request body, if somehow chat-bot is able to supply more info.
                .syncBody(Map.of(
                        "short_description", shortDescription,
                        "caller_id", callerEmailId))
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .map(doc -> doc.read("$.result.number"));
    }
}

