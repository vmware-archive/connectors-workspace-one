/*
 * Project Service Now Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.exception;

public class DiscoveryMetaDataReadFailedException extends RuntimeException {

    private static final long serialVersionUID = -5886501656889949051L;

    public DiscoveryMetaDataReadFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
