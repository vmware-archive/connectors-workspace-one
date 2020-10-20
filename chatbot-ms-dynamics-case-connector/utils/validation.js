/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const validateBackendHeaders = (req, res, next) => {
    if (!res.locals.backendBaseUrl) {
        return res.status(400).send({ message: 'The x-connector-base-url is required' })
    }

    if (!res.locals.backendAuthorization) {
        return res.status(400).send({ message: 'The x-connector-authorization is required' })
    }
    next()
}

module.exports = {
    validateBackendHeaders
}
