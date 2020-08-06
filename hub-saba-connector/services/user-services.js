/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const fetch = require('node-fetch')
const utils = require('../utils/utils')

const retrieveEmployeeId = async (res) => {
  const options = {
    headers: {
      SabaCertificate: res.locals.sabaCertificate
    }
  }
  const queryQ = encodeURIComponent(`(username==${res.locals.mfJwt.email})`)
  return fetch(`${res.locals.backendBaseUrl}/v1/people?type=internal&q=${queryQ}&f=(id)`, options)
    .then(utils.withExceptionForHttpError)
    .then(response => response.json())
}

module.exports = {
  retrieveEmployeeId
}
