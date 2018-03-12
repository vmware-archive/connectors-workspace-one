/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

const yaml = require('js-yaml');
const fs = require('fs');
const options = require('./command-line-options');

// https://www.npmjs.com/package/js-yaml
const managedApps = yaml.safeLoad(fs.readFileSync(options.configFile));

module.exports = Object.freeze(managedApps);
