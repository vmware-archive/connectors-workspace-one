package com.vmware.connectors.coupa.domain;
/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
import com.fasterxml.jackson.annotation.JsonProperty;

public class Currency {

	@JsonProperty("id")
	private String id;
	@JsonProperty("code")
	private String code;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
