/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur.response;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Concur action response to indicate whether approve or reject action was successful or not.
 */
@XmlRootElement(name = "ActionStatus", namespace = "http://www.concursolutions.com/api/expense/expensereport/2011/03")
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("PMD.UnnecessaryConstructor")
public class ConcurResponse {

    @XmlElement(name = "Message")
    private String message;

    @XmlElement(name = "Status")
    private String status;

    public ConcurResponse() {
        // Empty constructor.
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
