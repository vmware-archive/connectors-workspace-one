package com.vmware.connectors.common.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HashUtil {

    private static final String EMPTY_LIST = DigestUtils.sha1Hex("empty-list");
    private static final String NULL_LIST = DigestUtils.sha1Hex("null-list");

    private static final String EMPTY_MAP = DigestUtils.sha1Hex("empty-map");
    private static final String NULL_MAP = DigestUtils.sha1Hex("null-map");

    public static String hash(final Object... args) {
        final StringBuilder result = new StringBuilder();

        for (final Object arg : args) {
            result.append(arg);
        }

        return DigestUtils.sha1Hex(result.toString());
    }

    public static String hashList(final List<String> argList) {
        if (argList == null) {
            return NULL_LIST;
        }

        if (argList.isEmpty()) {
            return EMPTY_LIST;
        }

        final List<String> result = argList.stream()
                .sorted()
                .map(arg -> DigestUtils.sha1Hex(StringUtils.EMPTY + arg))
                .collect(Collectors.toList());
        return DigestUtils.sha1Hex(result.toString());
    }

    public static String hashMap(final Map<String, String> argMap) {
        if (argMap == null) {
            return NULL_MAP;
        }

        if (argMap.isEmpty()) {
            return EMPTY_MAP;
        }


        final List<String> result = argMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entrySet -> DigestUtils.sha1Hex(StringUtils.EMPTY + entrySet.getKey())
                        + DigestUtils.sha1Hex(StringUtils.EMPTY + entrySet.getValue()))
                .collect(Collectors.toList());
        return DigestUtils.sha1Hex(result.toString());
    }

    private HashUtil() {
        // Empty constructor.
    }
}
