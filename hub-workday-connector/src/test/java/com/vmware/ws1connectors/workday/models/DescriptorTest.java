/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings
public class DescriptorTest {
    private static final String DESCRIPTOR = "Absence Request: Ashton Burns";
    private static final String ID = "fc844b7a8f6f01580738a5ffd6115105";
    private static final String HREF = "https://workday.com/ccx/api/v1/tenant/timeOffRequest/" + ID;

    private Descriptor descriptor;

    @BeforeEach public void setup() {
        descriptor = Descriptor.builder()
            .descriptor(DESCRIPTOR)
            .href(HREF)
            .id(ID)
            .build();
    }

    @Nested public class GetDescriptor {
        @Test public void returnsDescriptor() {
            assertThat(descriptor.getDescriptor()).isEqualTo(DESCRIPTOR);
        }
    }

    @Nested public class GetId {
        @Test public void returnsFullName() {
            assertThat(descriptor.getId()).isEqualTo(ID);
        }
    }

    @Nested public class GetHref {
        @Test public void returnsWorkerID() {
            assertThat(descriptor.getHref()).isEqualTo(HREF);
        }
    }

}
