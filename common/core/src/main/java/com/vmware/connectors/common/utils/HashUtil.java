package com.vmware.connectors.common.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HashUtil {

    public static final String SPACE_SHA1_VALUE = DigestUtils.sha1Hex(StringUtils.SPACE);

    public static String hash(final Object... args) {
        final StringBuilder result = new StringBuilder();

        for (final Object arg : args) {
            result.append(arg);
        }

        return DigestUtils.sha1Hex(result.toString());
    }

    public static String hashList(final List<String> argList) {
        if (CollectionUtils.isEmpty(argList)) {
            return SPACE_SHA1_VALUE;
        }

        final List<String> result = argList.stream()
                .sorted()
                .map(arg -> DigestUtils.sha1Hex(StringUtils.EMPTY + arg))
                .collect(Collectors.toList());
        return DigestUtils.sha1Hex(result.toString());
    }

    public static String hashMap(final Map<String, String> argMap) {
        if (CollectionUtils.isEmpty(argMap)) {
            return SPACE_SHA1_VALUE;
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
