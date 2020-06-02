/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.ws1connectors.workday.exceptions.UserException;
import com.vmware.ws1connectors.workday.models.WorkdayUser;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.workday.test.FileUtils;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class UserServiceTest extends ServiceTestsBase {
    private static final String NO_EMAIL = null;
    private static final String EMAIL = "user1@example.com";
    private static final String USER2_EMAIL = "user2@example.com";

    private static final String USER_INFO = FileUtils.readFileAsString("user_info.json");
    private static final String NO_USER_INFO = FileUtils.readFileAsString("no_data_total_zero.json");
    private static final String NO_USER_INFO_TOTAL_NON_ZERO = FileUtils.readFileAsString("no_data_total_non_zero.json");

    @InjectMocks private UserService userService;

    @BeforeEach public void initialize() {
        setupRestClient(userService, "restClient");
    }

    private static Stream<Arguments> invalidInputsForGetUser() {
        return new ArgumentsStreamBuilder()
            .add(NO_BASE_URL, WORKDAY_TOKEN, EMAIL)
            .add(BASE_URL, NO_WORKDAY_TOKEN, EMAIL)
            .add(BASE_URL, WORKDAY_TOKEN, NO_EMAIL)
            .build();
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForGetUser")
    public void whenGetUserProvidedWithInvalidInputs(final String baseUrl, final String workdayAuth, final String email) {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> userService.getUser(baseUrl, workdayAuth, email));
        verifyWorkdayApiNeverInvoked();
    }

    @Test public void userDetailsFound() {
        mockWorkdayApiResponse(USER_INFO);

        final Mono<WorkdayUser> workdayUserMono = userService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);

        final WorkdayUser expectedWorkdayUser = JsonUtils.convertToWorkdayResourceFromJson(USER_INFO, WorkdayUser.class)
            .getData()
            .get(0);
        StepVerifier.create(workdayUserMono)
            .expectNext(expectedWorkdayUser)
            .verifyComplete();
    }

    private static List<String> userInfoResponses() {
        return Arrays.asList(USER_INFO, NO_USER_INFO, NO_USER_INFO_TOTAL_NON_ZERO);
    }

    @ParameterizedTest
    @MethodSource("userInfoResponses")
    public void noUserFound(final String userInfoResponse) {
        mockWorkdayApiResponse(userInfoResponse);
        Mono<WorkdayUser> workdayUserMono = userService.getUser(BASE_URL, WORKDAY_TOKEN, USER2_EMAIL);
        StepVerifier.create(workdayUserMono)
            .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "BAD_REQUEST"})
    public void errorOccursWhenGettingTheUser(HttpStatus httpStatus) {
        mockWorkdayApiErrorResponse(httpStatus);
        final Mono<WorkdayUser> workdayUserMono = userService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);

        StepVerifier.create(workdayUserMono)
            .expectError(UserException.class)
            .verify(DURATION_2_SECONDS);
    }

}
