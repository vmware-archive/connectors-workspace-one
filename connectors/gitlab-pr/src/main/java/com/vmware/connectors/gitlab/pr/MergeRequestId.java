/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.gitlab.pr;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * All Merge Requests and Notes have this information as part of their identity.
 */
public class MergeRequestId {

    private final String namespace;
    private final String projectName;
    private final String number;

    public MergeRequestId(String namespace, String projectName, String number) {
        this.namespace = namespace;
        this.projectName = projectName;
        this.number = number;
    }

    public String getProjectId() {
        return namespace + "%2F" + projectName;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getNumber() {
        return number;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
