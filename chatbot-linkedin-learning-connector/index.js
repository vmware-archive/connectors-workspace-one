/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { log } = require('./utils/log')

const express = require('express')
const discovery = require('./routes/discovery')
const botDiscovery = require('./routes/bot-discovery')

const utility = require('./utils/utility')
const botActions = require('./routes/bot-actions')

const connectorAuth = require('./utils/connector-auth')
const mfRouting = require('./utils/mf-routing')
const app = express()

app.use('/*', utility.handleXRequestId)
app.use('/bot/*', connectorAuth.validate, mfRouting.addContextPath, utility.validateReqHeaders)

app.use(express.json())
app.use(express.urlencoded({ extended: true }))
app.use(express.static('public'))

app.get('/health', (req, res) => res.json({status: 'UP'}))

app.get('/', discovery.root)

// get discovery for chat bot
app.post('/bot/discovery', botDiscovery.capabilities)

// show options to user to select services
app.get('/bot/actions/options-catalog', botActions.optionsCatalog)

// get top/trending courses from linkedIn learning
app.get('/bot/actions/top-picks', botActions.userTopPicks)

// get new courses from linkedIn learning
app.get('/bot/actions/new-courses', botActions.newCourses)

// get courses from linkedIn learning by user keyword search
app.post('/bot/actions/keyword-search', botActions.keywordSearch)

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
