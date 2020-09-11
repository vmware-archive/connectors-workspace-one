/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const withExceptionForHttpError = (response) => {
    if (response.ok) {
        return response
    } else {
        const error = new Error(response.statusText)
        error.status = response.status
        error.response = response
        throw error
    }
}

module.exports = {
    withExceptionForHttpError
}
