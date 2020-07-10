'use strict'

const { v4: uuid } = require('uuid')
const auth = require('./auth')

const handleXRequestId = (req, res, next) => {
    res.locals.xReqId = req.headers['x-request-id'] || 'hzrc-gen-' + uuid()
    return next()
}

const derivedBaseUrl = (req) => {
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

const setLocals = (req, res, next) => {
    res.locals.connectorAuthorization = req.header('x-connector-authorization')
    res.locals.authorization = req.header('authorization')
    res.locals.baseUrl = req.header('x-connector-base-url')
    next()
}

const initConnector = () => {
  auth.testConfig()
}

module.exports = {
    derivedBaseUrl,
    handleXRequestId,
    initConnector,
    setLocals
  }