/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const express = require('express')
const { log } = require('./utils/log')
const utility = require('./utils/utility')

const discovery = require('./routes/discovery')
const request = require('./routes/cards-request')

const connectorAuth = require('./utils/connector-auth')
const mfRouting = require('./utils/mf-routing')
const app = express()

app.use('/*', utility.handleXRequestId)
app.use('/api/*', connectorAuth.validate, mfRouting.addContextPath, utility.validateReqHeaders)

app.use(express.json())
app.use(express.urlencoded({ extended: true }))
app.use(express.static('public'))

app.get('/', discovery.root)

app.get('/health', (req, res) => res.json({status: 'UP'}))

// get new courses from linkedin learning
app.post('/api/cards', request.newCourses)

const port = process.env.PORT || 3000

try {
  utility.initConnector()
} catch (e) {
  log('Unable to initialize the connector. \n' + e.message)
  process.exit(1)
}

module.exports = app.listen(port, function () {
  log(`Connector listening on port ${port}.`)
})
