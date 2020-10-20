/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

let fakeBackend

const startFakeBackend = () => {
  const express = require('express')
  const app = express()
  app.use(express.json())
  fakeBackend = app.listen(4000, () => console.log('Fake backend listening on 4000'))

  app.use('/api/data/v9.0/WhoAmI', (req, res, next) => {
    const responseStatus = 200
    const dynamicsToken = req.header('authorization')
    if (dynamicsToken === 'DYNAMICS_TOKEN_FOR_USER_ONE') {
      return res.status(responseStatus).json(require('./dynamicsApi/response/user-with-single-case'))
    } else if (dynamicsToken === 'DYNAMICS_TOKEN_FOR_USER_TWO') {
      return res.status(responseStatus).json(require('./dynamicsApi/response/user-with-multiple-cases'))
    } else if ((dynamicsToken === 'DYNAMICS_TOKEN_FOR_USER_THREE')) {
      return res.status(responseStatus).json(require('./dynamicsApi/response/user-with-no-cases'))
    } else {
      next()
    }
  })

  app.use('/api/data/v9.0/incidents*', (req, res, next) => {
    const responseStatus = 200
    const dynamicsToken = req.header('authorization')
    if (dynamicsToken === 'DYNAMICS_TOKEN_FOR_USER_ONE') {
      return res.status(responseStatus).json(require('./dynamicsApi/response/single-case'))
    } else if (dynamicsToken === 'DYNAMICS_TOKEN_FOR_USER_TWO') {
      return res.status(responseStatus).json(require('./dynamicsApi/response/multiple-cases'))
    } else if (dynamicsToken === 'DYNAMICS_TOKEN_FOR_USER_THREE') {
      return res.status(responseStatus).json(require('./dynamicsApi/response/no-cases'))
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
