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

import java.io.IOException;
import java.util.*;

@JsonDeserialize(using = SFOpportunity.SFOpportunityDeserializer.class)
class SFOpportunity {

    private static final ObjectMapper mapper = new ObjectMapper();
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
            JsonNode recordsNode = rootNode.get("records");

            List<SFOpportunity> oppList = new ArrayList<>();
            if (recordsNode.isArray()) {
                for (JsonNode recNode : recordsNode) {
                    SFOpportunity opp = mapper.treeToValue(recNode, SFOpportunity.class);
                    oppList.add(opp);
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

            opp.id = node.get(KEY_ID).asText();
            opp.name = node.get(KEY_NAME).asText();
            opp.closeDate = node.get(KEY_CLOSE_DATE).asText();
            opp.stageName = node.get(KEY_STAGE_NAME).asText();
            opp.amount = node.get(KEY_AMOUNT).asText();
            opp.nextStep = node.get(KEY_NEXT_STEP).asText();
            opp.expectedRevenue = node.get(KEY_EXPECTED_REVENUE).asText();
            opp.accountName = node.get(KEY_ACCOUNT).get(KEY_NAME).asText();
            opp.accountOwner = node.get(KEY_ACCOUNT).get(KEY_OWNER).get(KEY_NAME).asText();

            parseFeedsNode(node.get(KEY_FEEDS), opp);

            return opp;
        }

        private void parseFeedsNode(JsonNode feedsNode, SFOpportunity opp) {
            if (feedsNode != null && feedsNode.has(KEY_RECORDS)) {
                Iterator<JsonNode> feedsIter = feedsNode.get(KEY_RECORDS).elements();
                while (feedsIter.hasNext()) {
                    String feedEntry;
                    JsonNode fn = feedsIter.next();
                    String body = fn.get(KEY_BODY).asText();
                    if (StringUtils.isBlank(body)) {
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
    }
}
