/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const fetch = require('node-fetch')
const mfCommons = require('@vmw/mobile-flows-connector-commons')

const certificateCache = {}

const acquireSabaCertificate = async (req, res, next) => {
  if (!res.locals.backendBaseUrl) {
    return res.status(400).json({ message: 'Backend API base URL is required' })
  }

  try {
    res.locals.sabaCertificate = await getCertificate(res)
  } catch (e) {
    mfCommons.logReq(res, 'Failed to acquire Saba certificate for API authorization. ' + e.message)
    return res.status(400).json({ message: 'Failed to acquire Saba certificate. Please Confirm service credentials are correct' })
  }

  next()
}

const getCertificate = async (res) => {
  const mfTenantId = res.locals.mfJwt.tenantId

  if (certificateCache[mfTenantId] && certificateCache[mfTenantId].certificate && certificateCache[mfTenantId].expiresAt > Date.now()) {
    return certificateCache[mfTenantId].certificate
  }

  const serviceCredentials = res.locals.backendAuthorization

  validateServiceCredentaialsPattern(serviceCredentials)
  const { username, password } = readServiceCredentials(serviceCredentials)
  const newCert = await retrieveNewCertificate(res, res.locals.backendBaseUrl, username, password)

  certificateCache[mfTenantId] = {
    certificate: newCert.certificate,
    expiresAt: Date.now() + getCacheExpiration()
  }

  return certificateCache[mfTenantId].certificate
}

const getCacheExpiration = () => {
  const cacheExpirationHrs = 24
  return cacheExpirationHrs * 60 * 60 * 1000
}

const retrieveNewCertificate = async (res, baseUrl, username, password) => {
  mfCommons.logReq(res, 'Retrieve a new Saba certificate.')
  const options = {
    headers: {
      user: username,
      password: password
    }
  }
  return fetch(`${baseUrl}/v1/login`, options)
    .then(response => {
      if (response.ok) {
        return response.json()
      } else {
        const error = new Error(response.statusText)
        error.status = response.status
        error.response = response
        throw error
      }
    })
}

const validateServiceCredentaialsPattern = (serviceCredentials) => {
  if (serviceCredentials.split(':').length !== 2) {
    throw new Error('Invalid service credentials configuration. Please use the format <username>:<password>')
  }
}

const readServiceCredentials = (serviceCredentials) => {
  const [username, password] = serviceCredentials.split(':')
  return { username, password }
}

const clearTokens = (res) => {
  mfCommons.logReq(res, 'Clearing cached Saba certificate for the tenant.')
  const mfTenantId = res.locals.mfJwt.tenantId
  certificateCache[mfTenantId].certificate = null
}

module.exports = {
  clearTokens,
  acquireSabaCertificate
}
