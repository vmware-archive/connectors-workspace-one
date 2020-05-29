'use strict'

const util = require('util')
const testUtils = require('./test-utils')

const clientId = 'test-client-id'
const clientSecret = 'test-client-secret'

const oAuthAccessToken = 'test-backend-token'
const oAuthAccessTokenNew = 'new-backend-token' // for token refresh.
const allowedAccessTokens = [oAuthAccessToken, oAuthAccessTokenNew]

let fakeBackend

const getClientCredConfig = function () {
  return `${clientId}:${clientSecret}`
}

const startFakeBackend = () => {
  const express = require('express')
  const app = express()
  app.use(express.json())

  // nothing to do now, will update in later pr
  app.use('/oauth/*', (req, res, next) => {
    return next()
  })

  // nothing to do now, will update in later pr
  app.post('/oauth/v2/accessToken', (req, res) => {
    return res.json(
      getTokenResponse(req.query.username, req.query.grant_type)
    )
  })

  app.get('/learningAssets', (req, res) => {
    const requestId = req.headers['x-request-id']
    if (requestId === 'userzero') {
      return res.json(
        require('./linkedin/response/noResultCourse')
      )
    }

    const expDiscovery = require('./linkedin/response/course')
    return res.json(
      expDiscovery
    )
  })
  fakeBackend = app.listen(4000, () => console.log('Fake backend listening on 4000'))
}
const getTokenResponse = (serviceUser, grantType) => {
  const expiresIn = 3
  const accessToken = oAuthAccessToken
  return {
    access_token: accessToken,
    expires_in: expiresIn
  }
}

const start = () => {
  startFakeBackend()
}

const stop = (fn) => {
  fakeBackend.close(() => {
    fn()
  })
}

module.exports = {
  start,
  stop,
  getClientCredConfig
}
