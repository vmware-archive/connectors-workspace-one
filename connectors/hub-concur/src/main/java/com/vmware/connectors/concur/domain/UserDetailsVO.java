package com.vmware.connectors.concur.domain;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDetailsVO implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 5424856972480327687L;

    private String loginId;


    @JsonProperty("LoginID")
    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

}
