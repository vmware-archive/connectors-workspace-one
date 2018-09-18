/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import java.util.List;
import java.util.Map;

@SuppressWarnings("PMD")
class FakeData {

    static final List<Map<String, Object>> ALL_REQUESTS = List.of(
            Map.of(
                    "id", "1",
                    "status", "open"
            ),
            Map.of(
                    "id", "2",
                    "status", "approved"
            ),
            Map.of(
                    "id", "3",
                    "status", "rejected"
            ),
            Map.of(
                    "id", "4",
                    "status", "open"
            )
    );

    static final List<Map<String, Object>> ALL_REQUEST_DATA = List.of(
            Map.of(
                    "id", "1",
                    "status", "open",
                    "report_name", "Expense Report One",
                    "project_number", "1000",
                    "submission_date", "2018-09-18T12:13:55.277Z",
                    "requested_by", "Ryan",
                    "cost_center", "R & D",
                    "expense_amount", "$100"
            ),
            Map.of(
                    "id", "2",
                    "status", "approved",
                    "report_name", "Expense Report Two",
                    "project_number", "1000",
                    "submission_date", "2017-09-18T12:13:55.277Z",
                    "requested_by", "Joe",
                    "cost_center", "R & D",
                    "expense_amount", "$200"
            ),
            Map.of(
                    "id", "3",
                    "status", "rejected",
                    "report_name", "Expense Report Three",
                    "project_number", "2000",
                    "submission_date", "2017-07-18T12:13:55.277Z",
                    "requested_by", "Helen",
                    "cost_center", "R & D",
                    "expense_amount", "$300"
            ),
            Map.of(
                    "id", "4",
                    "status", "open",
                    "report_name", "Expense Report Four",
                    "project_number", "2000",
                    "submission_date", "2018-07-18T12:13:55.277Z",
                    "requested_by", "Jane",
                    "cost_center", "R & D",
                    "expense_amount", "$400"
            )
    );

}
