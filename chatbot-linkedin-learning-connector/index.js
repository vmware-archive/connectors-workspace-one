/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const express = require('express')
const discovery = require('./routes/discovery')
const botDiscovery = require('./routes/bot-discovery')
const utility = require('./utils/utility')
const botActions = require('./routes/bot-actions')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const app = express()

app.use(express.json())
app.use(express.urlencoded({ extended: true }))
app.use(express.static('public'))

const publicKeyUrl = process.env.MF_JWT_PUB_KEY_URI
if (!publicKeyUrl) {
  throw Error('Please provide Mobile Flows public key URL at MF_JWT_PUB_KEY_URI')
}

app.use('/*', mfCommons.handleXRequestId)
app.use('/bot/*', mfCommons.validateAuth(publicKeyUrl), mfCommons.readBackendHeaders, mfCommons.mfRouting.addContextPath, utility.validateReqHeaders)
app.get('/health', (req, res) => res.json({ status: 'UP' }))

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

module.exports = app.listen(port, function () {
  mfCommons.log(`Connector listening on port ${port}.`)
})
