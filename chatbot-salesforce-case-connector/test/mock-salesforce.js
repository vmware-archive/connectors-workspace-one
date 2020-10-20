/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

let fakeBackend
const mfCommons = require('@vmw/mobile-flows-connector-commons')

const startFakeBackend = () => {
  const express = require('express')
  const app = express()
  app.use(express.json())
  fakeBackend = app.listen(4000, () => mfCommons.log('Fake backend listening on 4000'))

  app.use('/services/oauth2/userinfo', (req, res, next) => {
    const responseStatus = 200
    const salesforceToken = req.header('authorization')
    if (salesforceToken === 'SALESFORCE_TOKEN_FOR_USER_ONE') {
      return res.status(responseStatus).json(require('./salesforceApi/response/user-with-single-case'))
    } else if (salesforceToken === 'SALESFORCE_TOKEN_FOR_USER_TWO') {
      return res.status(responseStatus).json(require('./salesforceApi/response/user-with-multiple-cases'))
    } else if ((salesforceToken === 'SALESFORCE_TOKEN_FOR_USER_THREE')) {
      return res.status(responseStatus).json(require('./salesforceApi/response/user-with-no-cases'))
    } else {
      next()
    }
  })

  app.use('/services/data/v47.0/query*', (req, res, next) => {
    const responseStatus = 200
    const salesforceToken = req.header('authorization')
    if (salesforceToken === 'SALESFORCE_TOKEN_FOR_USER_ONE') {
      return res.status(responseStatus).json(require('./salesforceApi/response/single-case'))
    } else if (salesforceToken === 'SALESFORCE_TOKEN_FOR_USER_TWO') {
      return res.status(responseStatus).json(require('./salesforceApi/response/multiple-cases'))
    } else if (salesforceToken === 'SALESFORCE_TOKEN_FOR_USER_THREE') {
      return res.status(responseStatus).json(require('./salesforceApi/response/no-cases'))
    }
    next()
  })
}

const start = () => {
  startFakeBackend()
}

const stop = () => {
  fakeBackend.close()
}

module.exports = {
  start,
  stop
}
