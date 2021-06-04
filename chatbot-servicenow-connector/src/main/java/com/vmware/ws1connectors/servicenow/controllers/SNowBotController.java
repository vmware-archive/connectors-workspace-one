/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.controllers;

import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.servicenow.bot.discovery.capabilities.BotCapability;
import com.vmware.ws1connectors.servicenow.constants.WorkflowId;
import com.vmware.ws1connectors.servicenow.domain.BotObjects;
import com.vmware.ws1connectors.servicenow.utils.BotObjectBuilderUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.BOT_DISCOVERY_URL;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.CHILDREN;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.ITEM_DETAILS;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.OBJECTS;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.OPERATION_CANCEL_URL;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.ROUTING_PREFIX;

@RestController
@Slf4j
public class SNowBotController extends BaseController {

    private final ConnectorTextAccessor connectorTextAccessor;

    @Autowired public SNowBotController(ConnectorTextAccessor connectorTextAccessor) {
        super();

        this.connectorTextAccessor = connectorTextAccessor;
    }

    // An object, 'botDiscovery', advertises all the capabilities of this connector, for bot use cases.
    // ToDo: After 1 flow works en-end, advertise remaining capabilities as well.
    @PostMapping(
            path = BOT_DISCOVERY_URL,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> getBotDiscovery(
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            @Valid @RequestBody final CardRequest cardRequest,
            Locale locale
    ) {
        Map<String, String> connectorConfigMap = cardRequest.getConfig();
        return ResponseEntity.ok(
                buildBotDiscovery(connectorConfigMap, routingPrefix, locale)
        );
    }

    private Map<String, Object> buildBotDiscovery(Map<String, String> connectorConfigMap, String routingPrefix, Locale locale) {
        return Map.of(OBJECTS, List.of(
                Map.of(
                        CHILDREN,
                        Arrays.stream(WorkflowId.values()).map(workflowIdEnum -> Map.of(ITEM_DETAILS,
                            BotCapability.build(workflowIdEnum, connectorTextAccessor, connectorConfigMap)
                                    .describe(routingPrefix, locale)))
                )
                )
        );
    }

    @PostMapping(
            path = OPERATION_CANCEL_URL,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public BotObjects cancelOperation(
            Locale locale) {
        LOGGER.trace("canceled an operation.");
        return BotObjectBuilderUtils.cancelObject(connectorTextAccessor, locale);
    }
}

