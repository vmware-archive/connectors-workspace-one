/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.ws1connectors.workday.exceptions.InboxTaskException;
import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.models.WorkdayUser;
import com.vmware.ws1connectors.workday.web.resources.WorkdayResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.WORKERS_INBOX_TASKS_API;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASKS_SUMMARY;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASKS_VIEW_QUERY_PARAM_NAME;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.WORKDAY_ACCESS_TOKEN;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.WORKDAY_BASE_URL;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotBlank;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@Slf4j
public class InboxService {
    private static final ParameterizedTypeReference<WorkdayResource<InboxTask>> WORKDAY_RESOURCE_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };
    private static final String USER = "User";

    @Autowired private WebClient restClient;

    public Flux<InboxTask> getTasks(final String baseUrl, final String workdayAccessToken, final WorkdayUser user) {
        checkArgumentNotBlank(baseUrl, WORKDAY_BASE_URL);
        checkArgumentNotBlank(workdayAccessToken, WORKDAY_ACCESS_TOKEN);
        checkArgumentNotNull(user, USER);

        LOGGER.info("Getting inbox tasks for user with userName: {}, workerId: {}, workdayId: {}.", user.getUserName(), user.getWorkerID(), user.getWorkdayID());
        final String inboxTaskUrl = buildInboxTasksUrl(baseUrl);

        LOGGER.info("Executing request to get inbox tasks url: {}", inboxTaskUrl);
        return restClient.get()
            .uri(inboxTaskUrl)
            .accept(APPLICATION_JSON)
            .header(AUTHORIZATION, workdayAccessToken)
            .retrieve()
            .onStatus(HttpStatus::isError, errorResponse -> {
                LOGGER.error("Resulted in error code {} for url {}", errorResponse.statusCode(), inboxTaskUrl);
                return Mono.error(() -> new InboxTaskException(errorResponse.statusCode()));
            })
            .bodyToMono(WORKDAY_RESOURCE_TYPE_REFERENCE)
            .filter(WorkdayResource::hasData)
            .flatMapIterable(WorkdayResource::getData);
    }

    private String buildInboxTasksUrl(String baseUrl) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path(WORKERS_INBOX_TASKS_API)
            .queryParam(INBOX_TASKS_VIEW_QUERY_PARAM_NAME, INBOX_TASKS_SUMMARY)
            .build()
            .toUriString();
    }

}
