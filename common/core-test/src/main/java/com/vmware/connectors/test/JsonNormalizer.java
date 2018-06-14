/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.test;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;

import static com.vmware.connectors.utils.IgnoredFieldsReplacer.*;

public final class JsonNormalizer {

    private final static Configuration configuration = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .build();

    private JsonNormalizer() {
        // do not construct
    }

    public static String forCards(String body) {
        DocumentContext context = JsonPath.using(configuration).parse(body);
        context.set("$.objects[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID);
        context.set("$.objects[?(@.creation_date =~ /" + DATE_PATTERN + "/)].creation_date", DUMMY_DATE_TIME);
        context.set("$.objects[?(@.expiration_date =~ /" + DATE_PATTERN + "/)].expiration_date", DUMMY_DATE_TIME);
        context.set("$.objects[*].actions[?(@.id =~ /" + UUID_PATTERN + "/)].id", DUMMY_UUID);
        return context.jsonString();
    }
}
