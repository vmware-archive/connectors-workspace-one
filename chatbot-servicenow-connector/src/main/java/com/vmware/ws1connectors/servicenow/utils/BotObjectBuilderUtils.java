/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.domain.BotAction;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.domain.BotObjects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_CONFIRMATION;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_TEXT;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotObjectBuilderUtils {
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String WORKFLOW_STEP_MSG = "workflowStep can't be null";
    private static final String TYPE = "type";
    private static final String OPERATION_CANCEL = "operation.cancel";

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

    public static BotObjects confirmationObject(ConnectorTextAccessor connectorTextAccessor,
                                                 BotActionBuilder botActionBuilder,
                                                 String descriptor,
                                                 String routingPrefix,
                                                 Locale locale,
                                                 BotAction confirmAction) {
        checkNotNull(connectorTextAccessor);
        checkNotNull(botActionBuilder);

        return new BotObjects.Builder()
                .addObject(new BotItem.Builder()
                        .setTitle(connectorTextAccessor.getTitle(descriptor, locale))
                        .setDescription(connectorTextAccessor.getDescription(descriptor, locale))
                        .setType(UI_TYPE_TEXT)
                        .setWorkflowStep(WorkflowStep.INCOMPLETE)
                        .addAction(confirmAction)
                        .addAction(botActionBuilder.declineWorkflow(routingPrefix, locale))
                        .build())
                .build();
    }

    public static BotObjects cancelObject(ConnectorTextAccessor connectorTextAccessor, Locale locale) {
        checkNotNull(connectorTextAccessor);

        return new BotObjects.Builder()
                .addObject(new BotItem.Builder()
                .setTitle(connectorTextAccessor.getTitle(OPERATION_CANCEL, locale))
                .setDescription(connectorTextAccessor.getDescription(OPERATION_CANCEL, locale))
                .setType(UI_TYPE_CONFIRMATION)
                .setWorkflowStep(WorkflowStep.COMPLETE)
                .build())
                .build();
    }
}
