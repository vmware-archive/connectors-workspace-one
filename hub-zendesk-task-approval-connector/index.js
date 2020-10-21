/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'
const express = require('express')
const path = require('path')
require('dotenv').config()
const config = require('./config/config')
const discoveryHandler = require('./routes/discovery-controller').discoveryController
const { requestLogger, validateReqHeaders } = require('./middleware/connector-req-pre-processor')
const cardsController = require('./routes/cards-request')
const actionsController = require('./routes/actions-controller')
const mfCommons = require('@vmw/mobile-flows-connector-commons')

const authenticatedAPIs = [
  '/cards',
  '/actions/approve',
  '/actions/decline'
]

const app = express()
app.set('trust proxy', true)
app.use(express.json())
app.use(express.urlencoded({ extended: true }))

app.use('*', mfCommons.handleXRequestId, requestLogger)

const publicKeyUrl = process.env.MF_JWT_PUB_KEY_URI
if (!publicKeyUrl) {
  throw Error('Please provide Mobile Flows public key URL at MF_JWT_PUB_KEY_URI')
}

app.use(authenticatedAPIs, mfCommons.validateAuth(publicKeyUrl), mfCommons.readBackendHeaders, mfCommons.mfRouting.addContextPath, validateReqHeaders)
app.get('/health', (_, res) => res.json({ status: 'UP' }))

//= =========== Start: Route end point configuration==============
// Discovery route
app.get('/', discoveryHandler)

// Cards route
app.post('/cards', cardsController.cardsController)

// Action routes
app.post('/actions/approve', actionsController.approveRequest)
app.post('/actions/decline', actionsController.declineRequest)

//= ==================END=========================================

app.use(express.static(path.join(__dirname, 'public')))

const port = process.env.PORT || config.DEFAULT_PORT
module.exports = app.listen(port, () => {
  mfCommons.log(`Connector listening on port ${port}.`)
})
