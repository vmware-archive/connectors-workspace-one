/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

public class JsonSchemaValidator {

    private static final Logger logger = LoggerFactory.getLogger(JsonSchemaValidator.class);

    private static final String CONNECTOR_CARD_RESPONSE_SCHEMA_DOC = "/schemata/herocard-connector-response-schema.json";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

    private final JsonSchema schema;

    public static JsonSchemaValidator getConnectorCardResponseSchemaValidator() throws IOException, ProcessingException {
        return new JsonSchemaValidator(new ClassPathResource(CONNECTOR_CARD_RESPONSE_SCHEMA_DOC));
    }

    public static Matcher<String> isValidHeroCardConnectorResponse() {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object item) {
                try {
                    return getConnectorCardResponseSchemaValidator().validate(mapper.readTree(item.toString()));
                } catch (ProcessingException | IOException e) {
                    logger.error(e.getMessage());
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                // Not much to say here
            }
        };
    }

    public JsonSchemaValidator(String jsonString) throws IOException, ProcessingException {
        JsonNode schemaJson = mapper.readTree(jsonString);
        schema = factory.getJsonSchema(schemaJson);
    }

    public JsonSchemaValidator(Resource res) throws IOException, ProcessingException {
        JsonNode schemaJson = mapper.readTree(res.getInputStream());
        schema = factory.getJsonSchema(schemaJson);
    }

    public boolean validate(JsonNode docNode) throws ProcessingException {
        ProcessingReport report = schema.validate(docNode);
        boolean valid = report.isSuccess();
        if (!valid) {
            logger.error("Processing report: {}", report);
        }
        return valid;
    }
}
