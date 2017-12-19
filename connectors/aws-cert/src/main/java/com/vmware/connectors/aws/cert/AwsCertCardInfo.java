/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.aws.cert;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

import java.util.Map;

@AutoProperty
class AwsCertCardInfo {

    private String domain;
    private String accountId;
    private String regionName;
    private String certIdentifier;
    private Map<String, String> formParams;

    String getDomain() {
        return domain;
    }

    void setDomain(String domain) {
        this.domain = domain;
    }

    String getAccountId() {
        return accountId;
    }

    void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    String getRegionName() {
        return regionName;
    }

    void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    String getCertIdentifier() {
        return certIdentifier;
    }

    void setCertIdentifier(String certIdentifier) {
        this.certIdentifier = certIdentifier;
    }

    public Map<String, String> getFormParams() {
        return formParams;
    }

    public void setFormParams(Map<String, String> formParams) {
        this.formParams = formParams;
    }

    @Override
    public String toString() {
        return Pojomatic.toString(this);
    }

}
