/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.controllers;

import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.exception.CatalogReadException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
public class BaseController {

    @ExceptionHandler(CatalogReadException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public Map<String, String> connectorDisabledErrorHandler(CatalogReadException e) {
        return Map.of(ServiceNowConstants.MESSAGE, e.getMessage());
    }
}
