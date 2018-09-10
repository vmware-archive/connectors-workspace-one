/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.utils;

@SuppressWarnings("PMD.ClassNamingConventions")
public class IgnoredFieldsReplacer {

    /**
     * A Pattern that matches Strings in UUID format. Version number is not checked, so invalid UUIDs can match
     * against this pattern.
     */
    public final static String UUID_PATTERN = "[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}";

    /**
     * A Pattern that matches Strings in date format (2017-05-06T07:10:34.000+00:00)
     */
    public final static String DATE_PATTERN = "^([\\+-]?\\d{4}(?!\\d{2}\\b))((-?)((0[1-9]|1[0-2])(\\3([12]\\d|0[1-9]|3[01]))?|W([0-4]\\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\\d|[12]\\d{2}|3([0-5]\\d|6[1-6])))([T\\s]((([01]\\d|2[0-3])((:?)[0-5]\\d)?|24\\:?00)([\\.,]\\d+(?!:))?)?(\\17[0-5]\\d([\\.,]\\d+)?)?([zZ]|([\\+-])([01]\\d|2[0-3]):?([0-5]\\d)?)?)?)?$";

    /**
     * A well-formed UUID string containing all zeroes. This is actually not a valid UUID because it does not have
     * a valid version number.
     */
    public static final String DUMMY_UUID = "00000000-0000-0000-0000-000000000000";


    /**
     * A well-formed ISO 8601 compliant Date
     */
    public static final String DUMMY_DATE_TIME = "1970-01-01T00:00:00Z";

}
