/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.ws1connectors.workday.exceptions.UserException;
import com.vmware.ws1connectors.workday.web.resources.WorkdayResource;
import com.vmware.ws1connectors.workday.models.WorkdayUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.USER_INFO;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkBasicConnectorArgumentsNotBlank;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@Slf4j
public class UserService {
    private static final ParameterizedTypeReference<WorkdayResource<WorkdayUser>> WORKDAY_RESOURCE_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };

    @Autowired private WebClient restClient;

    public Mono<WorkdayUser> getUser(final String baseUrl, final String workdayAccessToken, final String userEmail) {
        checkBasicConnectorArgumentsNotBlank(baseUrl, workdayAccessToken, userEmail);

        final String userInfoUri = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path(USER_INFO)
            .build()
            .toUriString();
        LOGGER.info("Executing request to get user details url: {}", userInfoUri);
        return restClient.get()
            .uri(userInfoUri)
            .accept(APPLICATION_JSON)
            .header(AUTHORIZATION, workdayAccessToken)
            .retrieve()
            .onStatus(HttpStatus::isError, errorResponse -> {
                LOGGER.error("Resulted in error code {} for url {}", errorResponse.statusCode(), userInfoUri);
                return Mono.error(() -> new UserException(errorResponse.statusCode()));
            })
            .bodyToMono(WORKDAY_RESOURCE_TYPE_REFERENCE)
            .filter(WorkdayResource::hasData)
            .flatMapIterable(WorkdayResource::getData)
            .filter(user -> equalsIgnoreCase(user.getEmail(), userEmail))
            .singleOrEmpty();
    }

}
