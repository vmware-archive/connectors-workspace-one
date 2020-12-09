/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.connectors.common.payloads.response;

import com.vmware.connectors.common.utils.HashUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.net.URI;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OpenInLink {
    private URI href;
    private String text;

    public String hash() {
        return HashUtil.hash(
            "href: ", this.href,
            "text: ", this.text
        );
    }
}
