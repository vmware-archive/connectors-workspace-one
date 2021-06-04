/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models;

import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SubjectTest {
    private Subject subject =
            JsonUtils.convertFromJsonFile("Business_Title_Change_Subject.json", Subject.class);

    @Test public void returnsSubjectDescriptor() {
        String subjectDescription = "Programmer/Analyst - Professional - Abhishek Anand (356404)";
        assertThat(subject.getDescriptor()).isEqualTo(subjectDescription);
    }
}
