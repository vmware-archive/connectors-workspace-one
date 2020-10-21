/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const mfCommons = require('@vmw/mobile-flows-connector-commons')

/**
 * @param  req - Request object
 * @param  res - Response object
 * @param  next - Express next function.
 */

const requestLogger = (req, res, next) => {
  res.on('finish', () => {
    mfCommons.logReq(res, `[method: ${req.method}]`, `[path: ${req.originalUrl}]`, `[status: ${res.statusCode}]`, `[xRequestId: ${res.locals.xRequestId}]`)
  })
  next()
}

const validateReqHeaders = (req, res, next) => {
  if (!res.locals.backendBaseUrl) {
    return res.status(400).send({ message: 'The x-connector-base-url is required' })
  }

  if (!res.locals.backendAuthorization) {
    return res.status(400).send({ message: 'The x-connector-authorization is required' })
  }
  next()
}

module.exports = {
  requestLogger,
  validateReqHeaders
}
