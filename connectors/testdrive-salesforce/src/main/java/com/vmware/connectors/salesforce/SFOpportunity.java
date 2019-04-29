/*
 * Copyright © 2019 VMware, Inc. All rights reserved. This product is protected by
 * copyright and intellectual property laws in the United States and other countries as
 * well as by international treaties. AirWatch products may be covered by one or more
 * patents listed at http://www.vmware.com/go/patents.
 */

package com.vmware.connectors.salesforce;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.jayway.jsonpath.InvalidJsonException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

@JsonDeserialize(using = SFOpportunity.SFOpportunityDeserializer.class)
class SFOpportunity {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(SFOpportunity.class);

    private static final String KEY_NAME = "Name";
    private static final String KEY_ID = "Id";
    private static final String KEY_CLOSE_DATE = "CloseDate";
    private static final String KEY_STAGE_NAME = "StageName";
    private static final String KEY_AMOUNT = "Amount";
    private static final String KEY_NEXT_STEP = "NextStep";
    private static final String KEY_EXPECTED_REVENUE = "ExpectedRevenue";
    private static final String KEY_ACCOUNT = "Account";
    private static final String KEY_OWNER = "Owner";
    private static final String KEY_FEEDS = "Feeds";
    private static final String KEY_RECORDS = "records";
    private static final String KEY_BODY = "Body";
    private static final String KEY_INSERTED_BY = "InsertedBy";
    private static final String NULL_STRING = "null";

    private String id;
    private String name;
    private String accountName;
    private String accountOwner;
    private String closeDate;
    private String stageName;
    private String amount;
    private String expectedRevenue;
    private String nextStep;
    private final List<String> feedEntries = new ArrayList<>();

    static List<SFOpportunity> fromJson(String json) {
        try {
            JsonNode rootNode = mapper.readTree(json);
            JsonNode recordsNode = rootNode.get(KEY_RECORDS);

            List<SFOpportunity> oppList = new ArrayList<>();
            if (recordsNode != null && recordsNode.isArray()) {
                for (JsonNode recNode : recordsNode) {
                    SFOpportunity opp = mapper.treeToValue(recNode, SFOpportunity.class);
                    if (opp != null) {
                        oppList.add(opp);
                    }
                }
            }
            return oppList;
        } catch (IOException e) {
            throw new InvalidJsonException(e);
        }
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getAccountName() {
        return accountName;
    }

    String getAccountOwner() {
        return accountOwner;
    }

    String getCloseDate() {
        return closeDate;
    }

    String getStageName() {
        return stageName;
    }

    String getAmount() {
        return amount;
    }

    String getExpectedRevenue() {
        return expectedRevenue;
    }

    String getNextStep() {
        return nextStep;
    }

    List<String> getFeedEntries() {
        return Collections.unmodifiableList(feedEntries);
    }

    static class SFOpportunityDeserializer extends StdDeserializer<SFOpportunity> {

        @SuppressWarnings("unused")
        SFOpportunityDeserializer() {
            this(null);
        }

        SFOpportunityDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public SFOpportunity deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {

            SFOpportunity opp = new SFOpportunity();

            JsonNode node = jsonParser.getCodec().readTree(jsonParser);

            opp.id = readNodeAtPath(node, KEY_ID);
            if (StringUtils.isBlank(opp.id)) {
                logger.warn("Salesforce opportunity response included an Opportunity with no ID");
                return null;
            }

            parseFields(opp, node);

            parseFeedsNode(node.get(KEY_FEEDS), opp);

            return opp;
        }

        // This method has been extracted from deserialize() to work around PMD's opinion of NCSS complexity.
        private void parseFields(SFOpportunity opp, JsonNode node) {
            opp.name = readNodeAtPath(node, KEY_NAME);
            opp.closeDate = readNodeAtPath(node, KEY_CLOSE_DATE);
            opp.stageName = readNodeAtPath(node, KEY_STAGE_NAME);
            opp.amount = readNodeAtPath(node, KEY_AMOUNT);
            opp.nextStep = readNodeAtPath(node, KEY_NEXT_STEP);
            opp.expectedRevenue = readNodeAtPath(node, KEY_EXPECTED_REVENUE);
            opp.accountName = readNodeAtPath(node, KEY_ACCOUNT, KEY_NAME);
            opp.accountOwner = readNodeAtPath(node, KEY_ACCOUNT, KEY_OWNER, KEY_NAME);
        }

        private void parseFeedsNode(JsonNode feedsNode, SFOpportunity opp) {
            if (feedsNode != null && feedsNode.has(KEY_RECORDS)) {
                Iterator<JsonNode> feedsIter = feedsNode.get(KEY_RECORDS).elements();
                while (feedsIter.hasNext()) {
                    String feedEntry;
                    JsonNode fn = feedsIter.next();
                    String body = fn.get(KEY_BODY).asText();
                    if (StringUtils.isBlank(body) || NULL_STRING.equalsIgnoreCase(body)) {
                        continue;
                    }
                    String commenterName = fn.get(KEY_INSERTED_BY).get(KEY_NAME).asText();
                    if (StringUtils.isBlank(commenterName)) {
                        feedEntry = body;
                    } else {
                        feedEntry = commenterName + " - " + body;
                    }
                    opp.feedEntries.add(feedEntry);
                }
            }
        }

        private String readNodeAtPath(JsonNode node, String... pathElements) {
            if (node == null || pathElements == null || pathElements.length == 0) {
                return null;
            }
            List<String> pathList = Arrays.asList(pathElements);
            Iterator<String> pathListIterator = pathList.listIterator();

            JsonNode targetNode = node;

            while (pathListIterator.hasNext()) {
                String pathElement = pathListIterator.next();
                targetNode = targetNode.get(pathElement);
                if (targetNode == null) {
                    return null;
                }
            }
            return targetNode.asText();
        }
    }
}
