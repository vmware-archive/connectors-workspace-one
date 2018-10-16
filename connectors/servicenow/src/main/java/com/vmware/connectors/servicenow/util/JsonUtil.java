package com.vmware.connectors.servicenow.util;

import java.util.Map;

public final class JsonUtil {

    private JsonUtil() {
        super();
    }

    public static String readJsonFromMap(Map<String, Object> keyVals) {

        String resultResponse = "{";
        for (Map.Entry<String, Object> keyVal : keyVals.entrySet()) {

            if (keyVal.getValue() instanceof String) {
                resultResponse = resultResponse + "\"" + keyVal.getKey() + "\"" + ":" + "\"" + keyVal.getValue() + "\",";
            } else if (keyVal.getValue() instanceof Map) {
                Map<String, Object> nestedObj = (Map<String, Object>)(keyVal.getValue());
                resultResponse = resultResponse + "\"" + keyVal.getKey() + "\"" + ":" + JsonUtil.readJsonFromMap(nestedObj) + ",";
            }
        }

        resultResponse = resultResponse.substring(0, resultResponse.length()-1) + "}";

        return resultResponse;
    }
}
