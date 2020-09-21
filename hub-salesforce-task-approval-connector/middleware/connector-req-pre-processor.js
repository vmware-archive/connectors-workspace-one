/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { connectorLogoURL } = require('../config/config')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const uuid = require('uuid/v4')

const setConnectorLocals = (req, res, next) => {
  res.locals.connectorLogoUrl = connectorLogoURL(res.locals.baseUrl)
  res.locals.apiBaseUrl = res.locals.baseUrl
  next()
}

const requestLogger = (req, res, next) => {
  res.on('finish', () => {
    mfCommons.logReq(res, `[method: ${req.method}]`, `[path: ${req.originalUrl}]`, `[status: ${res.statusCode}]`, `[xRequestId: ${res.locals.xRequestId}]`)
  })
  next()
}

const setLocals = (req, res, next) => {
  res.locals.xRequestId = req.headers['x-request-id'] || 'gen-' + uuid()
  res.locals.connectorAuthorization = req.headers['x-connector-authorization']
  res.locals.baseUrl = req.headers['x-connector-base-url']
  if (!res.locals.baseUrl) {
    return res.status(400).send({ message: 'The x-connector-base-url is required' })
  }
  if (!res.locals.connectorAuthorization) {
    return res.status(400).send({ message: 'The x-connector-authorization is required' })
  }

  next()
}

module.exports = {
  setConnectorLocals,
  requestLogger,
  setLocals
}

