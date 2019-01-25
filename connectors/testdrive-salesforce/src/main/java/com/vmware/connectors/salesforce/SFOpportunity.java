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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

@JsonDeserialize(using = SFOpportunity.SFOpportunityDeserializer.class)
public class SFOpportunity {

    private static final Logger logger = LoggerFactory.getLogger(TestDriveSalesforceController.class);

    private static final ObjectMapper mapper = new ObjectMapper();

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
            logger.error("Exception while trying to parse Salesforce opportunity data", e);
            return null;
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

            opp.id = node.get("Id").asText();
            opp.name = node.get("Name").asText();
            opp.closeDate = node.get("CloseDate").asText();
            opp.stageName = node.get("StageName").asText();
            opp.amount = node.get("Amount").asText();
            opp.nextStep = node.get("NextStep").asText();
            opp.expectedRevenue = node.get("ExpectedRevenue").asText();
            opp.accountName = node.get("Account").get("Name").asText();
            opp.accountOwner = node.get("Account").get("Owner").get("Name").asText();

            parseFeedsNode(node.get("Feeds"), opp);

            return opp;
        }

        private void parseFeedsNode(JsonNode feedsNode, SFOpportunity opp) {
            if (feedsNode != null && feedsNode.has("records")) {
                Iterator<JsonNode> feedsIter = feedsNode.get("records").elements();
                while (feedsIter.hasNext()) {
                    String feedEntry;
                    JsonNode fn = feedsIter.next();
                    String body = fn.get("Body").asText();
                    if (StringUtils.isBlank(body)) {
                        continue;
                    }
                    String commenterName = fn.get("InsertedBy").get("Name").asText();
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
