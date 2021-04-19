/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const moment = require('moment')
const fetch = require('node-fetch')

const getMyRecordings = async (res) => {
  const lastWeek = moment().subtract(7, 'days').format('YYYY-MM-DD')
  const today = moment().format('YYYY-MM-DD')
  const options = {
    headers: {
      Authorization: res.locals.backendAuthorization
    }
  }

  return fetch(`${res.locals.backendBaseUrl}/users/me/recordings?from=${lastWeek}&to=${today}`, options)
    .then(withExceptionForHttpError)
    .then(response => response.json())
}

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
  getMyRecordings
}
