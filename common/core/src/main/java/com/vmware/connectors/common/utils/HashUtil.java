package com.vmware.connectors.common.utils;

import org.apache.commons.codec.digest.DigestUtils;

public final class HashUtil {

    public static String hash(String... args) {
        final StringBuilder result = new StringBuilder();

        for (String arg: args) {
            result.append(arg);
        }

        return DigestUtils.sha1Hex(result.toString());
    }

    private HashUtil() {
        // Empty constructor.
    }
}
