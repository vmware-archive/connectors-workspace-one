/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur.exception;

public class AttachmentURLNotFoundException extends RuntimeException {
    public AttachmentURLNotFoundException(String message) {
        super(message);
    }
}
