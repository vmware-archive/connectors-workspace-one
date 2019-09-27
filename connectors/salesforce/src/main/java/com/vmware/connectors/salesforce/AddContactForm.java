/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.salesforce;

import javax.validation.constraints.NotBlank;
import java.util.Set;

@SuppressWarnings("PMD.MethodNamingConventions")
public class AddContactForm {
    @NotBlank
    private  String contactEmail;

    private String firstName;

    @NotBlank
    private String lastName;

    private Set<String> opportunityIds;

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContact_email(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirst_name(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLast_name(String lastName) {
        this.lastName = lastName;
    }

    public Set<String> getOpportunityIds() {
        return opportunityIds;
    }

    public void setOpportunity_ids(Set<String> opportunityIds) {
        this.opportunityIds = opportunityIds;
    }
}
