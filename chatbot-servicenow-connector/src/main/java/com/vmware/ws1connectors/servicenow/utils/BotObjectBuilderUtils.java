/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.domain.BotObjects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static com.google.common.base.Preconditions.checkNotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotObjectBuilderUtils {
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String WORKFLOW_STEP_MSG = "workflowStep can't be null";
    private static final String TYPE = "type";

    public static BotObjects botObjectBuilder(String title, String description, WorkflowStep workflowStep, String type) {
        ArgumentUtils.checkArgumentNotBlank(title, TITLE);
        ArgumentUtils.checkArgumentNotBlank(description, DESCRIPTION);
        ArgumentUtils.checkArgumentNotBlank(type, TYPE);
        checkNotNull(workflowStep, WORKFLOW_STEP_MSG);
        return new BotObjects.Builder()
                .addObject(new BotItem.Builder()
                        .setTitle(title)
                        .setDescription(description)
                        .setWorkflowStep(workflowStep)
                        .setType(type)
                        .build())
                .build();
    }
}
