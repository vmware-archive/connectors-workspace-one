/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict';

const commandLineArgs = require('command-line-args');

const optionDefinitions = [
    {
        name: 'configFile',
        type: String,
        defaultValue: '/usr/src/app/managed-apps.yml'
    },
    {
        name: 'greenBoxUrl',
        type: String
    },
    {
        name: 'vIdmPubKeyUrl',
        type: String
    }
];

const options = commandLineArgs(optionDefinitions);

module.exports = Object.freeze(options);
