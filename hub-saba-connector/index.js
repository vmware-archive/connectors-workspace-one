/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

const express = require('express')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const discovery = require('./routes/discovery')
const backendAuth = require('./utils/backend-auth')
const cardRequests = require('./routes/cards-requests')

const mfPublicKeyUrl = process.env.MF_JWT_PUB_KEY_URI
if (!mfPublicKeyUrl) {
  throw Error('Please provide Mobile Flows public key URL at MF_JWT_PUB_KEY_URI')
}

const app = express()
app.use(express.json())

app.use('/*', mfCommons.handleXRequestId)
app.use(['/api/*'], mfCommons.validateAuth(mfPublicKeyUrl))
app.use(['/api/*'], mfCommons.readBackendHeaders, backendAuth.acquireSabaCertificate)

app.get('/', discovery.root)
app.get('/health', (req, res) => res.json({ status: 'UP' }))
app.post('/api/cards/requests', cardRequests.handleCardRequest)

const port = process.env.PORT || 3000

module.exports = app.listen(port, function () {
  console.log(`Connector listening on port ${port}.`)
})
