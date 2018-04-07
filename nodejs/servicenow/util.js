/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

function log(format, ...args) {
    console.log('%s - ' + format, new Date().toISOString(), ...args);
}

exports.log = log;
