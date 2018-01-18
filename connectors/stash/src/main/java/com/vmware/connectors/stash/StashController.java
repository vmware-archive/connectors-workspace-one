/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.stash;

import com.vmware.connectors.common.utils.CardTextAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestOperations;

import javax.annotation.Resource;

@RestController
public class StashController {

    private static final Logger logger = LoggerFactory.getLogger(StashController.class);

    @Resource
    private AsyncRestOperations rest;

    @Resource
    private CardTextAccessor cardTextAccessor;
}
