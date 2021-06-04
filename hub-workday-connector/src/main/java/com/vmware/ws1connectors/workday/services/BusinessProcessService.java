/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.ws1connectors.workday.exceptions.BusinessProcessException;
import com.vmware.ws1connectors.workday.models.BusinessProcessTask;
import com.vmware.ws1connectors.workday.models.InboxTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.WORKDAY_ACCESS_TOKEN;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.WORKDAY_BASE_URL;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotBlank;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.LOCALE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@Slf4j
public class BusinessProcessService implements ApprovalTaskService {
    private static final String INBOX_TASK = "Inbox Task";
    @Autowired private WebClient restClient;

    @Override public Mono<BusinessProcessTask> getApprovalTaskDetails(String baseUrl, String workdayAccessToken,
                                                            InboxTask inboxTask, Locale locale) {
        checkArgumentNotBlank(baseUrl, WORKDAY_BASE_URL);
        checkArgumentNotBlank(workdayAccessToken, WORKDAY_ACCESS_TOKEN);
        checkArgumentNotNull(inboxTask, INBOX_TASK);
        checkArgumentNotNull(locale, LOCALE);

        return getBusinessProcessDetails(workdayAccessToken, inboxTask)
                .doOnNext(businessProcessTask -> businessProcessTask.setInboxTask(inboxTask));
    }

    private Mono<BusinessProcessTask> getBusinessProcessDetails(final String workdayAccessToken,
                                                                final InboxTask inboxTask) {
        LOGGER.info("Getting BusinessProcess details request: id: {}, intent: {}, assigned: {}, user: {}, link: {}",
                inboxTask.getId(), inboxTask.getDescriptor(), inboxTask.getAssigned(),
                inboxTask.getSubject().getDescriptor(), inboxTask.getHref());
        String businessProcessUrl = inboxTask.getOverallProcess().getHref();
        LOGGER.info("Executing request to get BusinessProcess details for request url: {}", businessProcessUrl);
        return restClient.get()
                .uri(businessProcessUrl)
                .accept(APPLICATION_JSON)
                .header(AUTHORIZATION, workdayAccessToken)
                .retrieve()
                .onStatus(HttpStatus::isError, errorResponse -> {
                    LOGGER.error("Resulted in error code {} for url {}", errorResponse.statusCode(),
                            businessProcessUrl);
                    return Mono.just(new BusinessProcessException(
                            "Error occurred while retrieving BusinessProcess details for inbox task",
                            errorResponse.statusCode()));
                })
                .bodyToMono(BusinessProcessTask.class);
    }
}
