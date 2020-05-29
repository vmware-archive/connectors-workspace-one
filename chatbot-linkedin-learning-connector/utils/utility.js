'use strict'

const uuid = require('uuid/v4')
const connectorAuth = require('./connector-auth')

const handleXRequestId = (req, res, next) => {
  res.locals.xReqId = req.headers['x-request-id'] || 'blic-gen-' + uuid()
  return next()
}

const initConnector = () => {
  connectorAuth.testConfig()
}

const validateReqHeaders = (req, res, next) => {
  res.locals.connectorAuthorization = req.header('x-connector-authorization')
  res.locals.baseUrl = req.header('x-connector-base-url')

  if (!res.locals.baseUrl) {
    return res.status(400).send({ message: 'The x-connector-base-url is required' })
  }

  if (!res.locals.connectorAuthorization) {
    return res.status(400).send({ message: 'The x-connector-authorization is required' })
  }
  next()
}

module.exports = {
  initConnector,
  handleXRequestId,
  validateReqHeaders
}
