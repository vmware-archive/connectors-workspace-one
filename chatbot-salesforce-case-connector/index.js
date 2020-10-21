/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const express = require('express')
const discovery = require('./routes/discovery')
const botDiscovery = require('./routes/bot-discovery')

const botActions = require('./routes/bot-actions')

const app = express()
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const validation = require('./utils/validation')

const mfPublicKeyUrl = process.env.MF_JWT_PUB_KEY_URI
if (!mfPublicKeyUrl) {
  throw Error('Please provide Mobile Flows public key URL at MF_JWT_PUB_KEY_URI')
}

app.use('/*', mfCommons.handleXRequestId)
app.use('/bot/*', mfCommons.validateAuth(mfPublicKeyUrl), mfCommons.mfRouting.addContextPath, mfCommons.readBackendHeaders, validation.validateBackendHeaders)

app.use(express.json())
app.use(express.urlencoded({ extended: true }))
app.use(express.static('public'))

app.get('/health', (req, res) => res.json({ status: 'UP' }))

app.get('/', discovery.root)

// get discovery for chat bot
app.post('/bot/discovery', botDiscovery.capabilities)

// get new cases from Salesforce
app.get('/bot/actions/new-cases', botActions.getPendingCases)

const port = process.env.PORT || 3000

module.exports = app.listen(port, () => {
  mfCommons.log(`Connector  on port ${port}.`)
})
