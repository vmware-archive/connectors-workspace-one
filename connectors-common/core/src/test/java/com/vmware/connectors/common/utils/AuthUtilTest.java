/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class AuthUtilTest {
    private static final String AUTH_TOKEN_PRE_HIRE_FALSE = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI1ZjM2NjM0Ny0xNWExLTQ3MGQtOWZjMi01NThhMTUzNWYxZDIiLCJhdWQiOiJodHRwOlwvXC9sb2NhbGhvc3Q6ODA4NVwvY2FyZHNcL3JlcXVlc3RzIiwiZW1sIjoiaGFyc2hhc0B2bXdhcmUuY29tIiwidXNlcl9uYW1lIjoiaGFyc2hhc0Bhdy10bWVocm90cmEudmlkbXByZXZpZXcuY29tW1N5c3RlbSBEb21haW5dIiwic2NvcGUiOlsiRU5EX1VTRVIiXSwiZG9tYWluIjoiU3lzdGVtIERvbWFpbiIsImV4cCI6MTU5NzA0MDk4OSwiaWF0IjoxNTk3MDQwNjg5LCJwcm4iOiJoYXJzaGFzQEFXLVRNRUhST1RSQSIsInRlbmFudCI6InQyIiwib2lzcyI6Imh0dHBzOlwvXC9hdy10bWVocm90cmEudmlkbXByZXZpZXcuY29tXC9TQUFTXC9hdXRoIiwicHJlX2hpcmUiOmZhbHNlfQ.j4JlhGHCAUf-mZS-H2s09-Qru2ZXXGOZA_Z0bf7mR4kJiSM5_wBc_iryjKxCrgQy5bMA9NbUyxoeOvNT8vUYFk4gGb3IAs59JepGco6wySRimXXsw8ojHn_z5aHCBbJedPwAsZfC1tYmsuJ29FvjGCJyYexLWXb_e7aV8kfz-6Yt3VRuBSc7uCPeodw7DrmVsuLzncVcD7OVYeK9a3IFn9RHICiX1Ip_3m__-2o4XNlmYZuKPxUGVT04yenUCw5jdKyJxAEQaPfm8tO404skfuKtW8ypr_7554_olISgLwqA9lz7GPQhRVt9IlbZhf_JoR4A2qfFK6uDXdkMl9u94g";
    private static final String AUTH_TOKEN_PRE_HIRE_TRUE = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1ZjM2NjM0Ny0xNWExLTQ3MGQtOWZjMi01NThhMTUzNWYxZDIiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjgwODUvY2FyZHMvcmVxdWVzdHMiLCJlbWwiOiJoYXJzaGFzQHZtd2FyZS5jb20iLCJ1c2VyX25hbWUiOiJoYXJzaGFzQGF3LXRtZWhyb3RyYS52aWRtcHJldmlldy5jb21bU3lzdGVtIERvbWFpbl0iLCJzY29wZSI6WyJFTkRfVVNFUiJdLCJkb21haW4iOiJTeXN0ZW0gRG9tYWluIiwiZXhwIjoxNTk3MjI1ODQ3LCJpYXQiOjE1OTcwNDA2ODksInBybiI6ImhhcnNoYXNAQVctVE1FSFJPVFJBIiwidGVuYW50IjoidDIiLCJvaXNzIjoiaHR0cHM6Ly9hdy10bWVocm90cmEudmlkbXByZXZpZXcuY29tL1NBQVMvYXV0aCIsInByZV9oaXJlIjp0cnVlLCJqdGkiOiI3OGJlMzA2Ny1mNTM5LTRlZmMtYjg5NS1lNjFjZDEwN2UwZjYifQ.SnguPNEL6E9sRui2hNAxfApgvyT1WzpYCPqsU0mHnxs";
    private static final String AUTH_TOKEN_PRE_HIRE_VALUE_IS_NOT_BOOLEAN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1ZjM2NjM0Ny0xNWExLTQ3MGQtOWZjMi01NThhMTUzNWYxZDIiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjgwODUvY2FyZHMvcmVxdWVzdHMiLCJlbWwiOiJoYXJzaGFzQHZtd2FyZS5jb20iLCJ1c2VyX25hbWUiOiJoYXJzaGFzQGF3LXRtZWhyb3RyYS52aWRtcHJldmlldy5jb21bU3lzdGVtIERvbWFpbl0iLCJzY29wZSI6WyJFTkRfVVNFUiJdLCJkb21haW4iOiJTeXN0ZW0gRG9tYWluIiwiZXhwIjoxNTk3MzEwNDM5LCJpYXQiOjE1OTcwNDA2ODksInBybiI6ImhhcnNoYXNAQVctVE1FSFJPVFJBIiwidGVuYW50IjoidDIiLCJvaXNzIjoiaHR0cHM6Ly9hdy10bWVocm90cmEudmlkbXByZXZpZXcuY29tL1NBQVMvYXV0aCIsInByZV9oaXJlIjoiSW52YWxpZFZhbHVlIiwianRpIjoiNWViMzE3ZTUtODdjYi00ZTU3LWJkNzMtNGRhYTRhYTVlZTFmIn0.LsnQNuooKl611npxMXUlwb7uNpPagoW-O4RJTtlsX1Q";
    private static final String AUTH_TOKEN_WITHOUT_PRE_HIRE = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIzYzVkNDk1OC03ODRhLTQ5OWItYTgxMS03MjYwNGJlZjg4N2EiLCJzY3AiOiJ1c2VyIHByb2ZpbGUgZW1haWwiLCJlbWwiOiJXczFAU1IuTUFOQUdFUi5BLmNvbSIsImN0eCI6Ilt7XCJtdGRcIjpcInVybjp2bXdhcmU6bmFtZXM6YWM6Y2xhc3NlczpMb2NhbFBhc3N3b3JkQXV0aFwiLFwiaWF0XCI6MTU5NzE0MTY1OCxcInR5cFwiOlwiMDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMDE0XCIsXCJpZG1cIjp0cnVlfV0iLCJpc3MiOiJodHRwczovL2h1Ym1mZXhwLnZzaWdub24uY29tL1NBQVMvYXV0aCIsInBpZCI6ImVjNWQ1NzRhLTQ0NTItNGFhOS05MmYwLTY1MmFhOGZiN2FhNCIsInBybiI6InNybWFuYWdlcmFASFVCTUZFWFAiLCJhdWQiOiJodHRwczovL2h1Ym1mZXhwLnZzaWdub24uY29tL1NBQVMvYXV0aC9vYXV0aHRva2VuIiwid2lkIjoiIiwiaWRwIjoiIiwidXNlcl9pZCI6IjEyMzQyMDciLCJhdXRoX3RpbWUiOjE1OTcxNDE2NTgsImRvbWFpbiI6IlN5c3RlbSBEb21haW4iLCJleHAiOjE1OTcxNzA0NTgsInBybl90eXBlIjoiVVNFUiIsImlhdCI6MTU5NzE0MTY1OCwiZGlkIjoiIiwianRpIjoiZWM1ZDU3NGEtNDQ1Mi00YWE5LTkyZjAtNjUyYWE4ZmI3YWE0IiwiY2lkIjoiIn0.THZrXzqt3HVGZZUwbuwc6xbHRK33oG50pR7yfHgXAL53x_kJipuvsxZLpnF2QdY9NjM5fuOks8mXgGcRtZDUAVepfrOQiz-SlYR65jtCB80rBMocLHQ9hGrF9wISEq4i1jps29Z0gzNlG4H9eabKe-KLPO5CtM0xmA_ZdNTBqK";
    private static final String AUTH_TOKEN_WITHOUT_EMAIL = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIzYzVkNDk1OC03ODRhLTQ5OWItYTgxMS03MjYwNGJlZjg4N2EiLCJzY3AiOiJ1c2VyIHByb2ZpbGUgZW1haWwiLCJjdHgiOiJbe1wibXRkXCI6XCJ1cm46dm13YXJlOm5hbWVzOmFjOmNsYXNzZXM6TG9jYWxQYXNzd29yZEF1dGhcIixcImlhdFwiOjE1OTcxNDE2NTgsXCJ0eXBcIjpcIjAwMDAwMDAwLTAwMDAtMDAwMC0wMDAwLTAwMDAwMDAwMDAxNFwiLFwiaWRtXCI6dHJ1ZX1dIiwiaXNzIjoiaHR0cHM6Ly9odWJtZmV4cC52c2lnbm9uLmNvbS9TQUFTL2F1dGgiLCJwaWQiOiJlYzVkNTc0YS00NDUyLTRhYTktOTJmMC02NTJhYThmYjdhYTQiLCJwcm4iOiJzcm1hbmFnZXJhQEhVQk1GRVhQIiwiYXVkIjoiaHR0cHM6Ly9odWJtZmV4cC52c2lnbm9uLmNvbS9TQUFTL2F1dGgvb2F1dGh0b2tlbiIsIndpZCI6IiIsImlkcCI6IiIsInVzZXJfaWQiOiIxMjM0MjA3IiwiYXV0aF90aW1lIjoxNTk3MTQxNjU4LCJkb21haW4iOiJTeXN0ZW0gRG9tYWluIiwiZXhwIjoxNTk3MzA5NjcxLCJwcm5fdHlwZSI6IlVTRVIiLCJpYXQiOjE1OTcxNDE2NTgsImRpZCI6IiIsImp0aSI6ImVjNWQ1NzRhLTQ0NTItNGFhOS05MmYwLTY1MmFhOGZiN2FhNCIsImNpZCI6IiJ9.jR49KSx6J92yTqo92h9yAnepkOuTUujNmyOMy36Q_oE";
    private static final String AUTH_TOKEN_MF = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1ZjM2NjM0Ny0xNWExLTQ3MGQtOWZjMi01NThhMTUzNWYxZDIiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjgwODUvY2FyZHMvcmVxdWVzdHMiLCJlbWwiOiJ1c2VyQGFiYy5jb20iLCJ1c2VyX25hbWUiOiJ1c2VyQGF3LWV4cC5jb21bU3lzdGVtIERvbWFpbl0iLCJkb21haW4iOiJTeXN0ZW0gRG9tYWluIiwiZXhwIjoxNTk3MzExNjMxLCJpYXQiOjE1OTcwNDA2ODksInBybiI6InVzZXJAQVctRVhQIiwidGVuYW50IjoidDIiLCJvaXNzIjoiaHR0cHM6Ly9hdy1leHAuY29tL1NBQVMvYXV0aCIsInByZV9oaXJlIjpmYWxzZSwianRpIjoiOWU4NDhiNzItZWQ2YS00NTA4LTgwNzYtNTdiZjQ2MmQyNGZjIn0.bnJXEfDPORk85W4b6cmIUualTgVowh2WRCmrOFp9sk8";

    @Test public void testExtractPreHire() {
        assertThat(AuthUtil.extractPreHire(AUTH_TOKEN_PRE_HIRE_FALSE)).isEqualTo(false);
        assertThat(AuthUtil.extractPreHire(AUTH_TOKEN_PRE_HIRE_TRUE)).isEqualTo(true);
    }

    @Test public void whenJwtTokenDoesNotContainPreHireAttributeReturnsFalse() {
        assertThat(AuthUtil.extractPreHire(AUTH_TOKEN_WITHOUT_PRE_HIRE)).isEqualTo(false);
    }

    @Test public void whenPreHireAttributeIsNotBooleanThenThrowsException() {
        assertThatExceptionOfType(ClassCastException.class)
                .isThrownBy(() -> AuthUtil.extractPreHire(AUTH_TOKEN_PRE_HIRE_VALUE_IS_NOT_BOOLEAN));
    }

    @Test public void testExtractEmail() {
        assertThat(AuthUtil.extractUserEmail(AUTH_TOKEN_WITHOUT_PRE_HIRE)).isEqualTo("Ws1@SR.MANAGER.A.com");
    }

    @Test public void whenEmailClaimNotPresentThenReturnsNull() {
        assertThat(AuthUtil.extractUserEmail(AUTH_TOKEN_WITHOUT_EMAIL)).isNull();
    }

    @Test public void testGetClaims() {
        Map<String, Object> expectedClaims = new HashMap<>();
        expectedClaims.put("sub", "5f366347-15a1-470d-9fc2-558a1535f1d2");
        expectedClaims.put("aud", "http://localhost:8085/cards/requests");
        expectedClaims.put("eml", "user@abc.com");
        expectedClaims.put("user_name", "user@aw-exp.com[System Domain]");
        expectedClaims.put("domain", "System Domain");
        expectedClaims.put("exp", 1597311631);
        expectedClaims.put("iat", 1597040689);
        expectedClaims.put("prn", "user@AW-EXP");
        expectedClaims.put("tenant", "t2");
        expectedClaims.put("oiss", "https://aw-exp.com/SAAS/auth");
        expectedClaims.put("pre_hire", false);
        expectedClaims.put("jti", "9e848b72-ed6a-4508-8076-57bf462d24fc");

        assertThat(AuthUtil.getClaims(AUTH_TOKEN_MF)).containsAllEntriesOf(expectedClaims);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void whenAuthTokenIsBlankThenThrowsException(String authTokenHeader) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> AuthUtil.getClaims(authTokenHeader));
    }
}
