package com.vmware.connectors.salesforce;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
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
                    JsonNode fn = feedsIter.next();
                    String entry = fn.get("InsertedBy").get("Name").asText() + " - " + fn.get("Body").asText();
                    opp.feedEntries.add(entry);
                }
            }

        }
    }
}
