'use strict'

const { v4: uuid } = require('uuid')
const connectorAuth = require('./connector-auth')
const LINKEDIN_LOGO_PATH = 'https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-linkedin-learning.png'

const handleXRequestId = (req, res, next) => {
  res.locals.xReqId = req.headers['x-request-id'] || 'hlic-gen-' + uuid()
  return next()
}

const initConnector = () => {
  connectorAuth.testConfig()
}

const urlPrefix = (req) => {
  const proto = req.header('x-forwarded-proto') || 'http'
  const host = req.header('x-forwarded-host')
  const port = req.header('x-forwarded-port')
  const path = req.header('x-forwarded-prefix') || ''

  if (host && port) {
    return `${proto}://${host}:${port}${path}`
  }

  if (host && !port) {
    return `${proto}://${host}${path}`
  }

  return `${proto}://${req.headers.host}${path}`
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
  urlPrefix,
  validateReqHeaders,
  LINKEDIN_LOGO_PATH
}
