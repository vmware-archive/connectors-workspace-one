/*
 * Copyright © 2017 VMware, Inc. All rights reserved. This product is protected by
 * copyright and intellectual property laws in the United States and other countries as
 * well as by international treaties. AirWatch products may be covered by one or more
 * patents listed at http://www.vmware.com/go/patents.
 */

package com.vmware.connectors.concur.response;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * Concur action response to indicate whether approve or reject action was successful or not.
 */
@XmlRootElement(name = "ActionStatus", namespace = "http://www.concursolutions.com/api/expense/expensereport/2011/03")
@XmlAccessorType(XmlAccessType.FIELD)
@AutoProperty
public class ConcurResponse implements Serializable {

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
        return Pojomatic.toString(this);
    }
}
