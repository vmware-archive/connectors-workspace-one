/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'
const express = require('express')
const path = require('path')

require('dotenv').config()
const config = require('./config/config')

const discoveryHandler = require('./routes/discovery').discoveryController
const { setConnectorLocals, requestLogger,setLocals } = require('./middleware/connector-req-pre-processor')
const mfCommons = require('@vmw/mobile-flows-connector-commons')

const cardsController = require('./routes/cards-request')
const actionsController = require('./routes/actions-controller')


const authenticatedAPIs = [
  '/cards',
  '/action/cancelCase',
  '/action/addNotes',
  '/action/resolveCase',
]
const app = express()
app.set('trust proxy', true)
app.use(express.json())
app.use(express.urlencoded({ extended: true }))

app.use('*', requestLogger)

app.use(authenticatedAPIs, mfCommons.mfRouting.addContextPath)
app.use(authenticatedAPIs, setLocals)
app.use(authenticatedAPIs, setConnectorLocals)

const publicKeyURL = process.env.MF_JWT_PUB_KEY_URI
app.use(authenticatedAPIs, mfCommons.validateAuth(publicKeyURL))
app.get('/health', (req, res) => res.json({status: 'UP'}))
//= =========== Start: Route end point configuration==============
// Discovery route
app.get('/', discoveryHandler)

// Cards route
app.post('/cards', cardsController.cardsController)

// Action routes
app.post('/action/resolveCase',actionsController.MarkCaseAsResolved)

app.post('/action/addNotes',actionsController.addNoteAboutCase)

app.post('/action/cancelCase',actionsController.MarkCaseAsCancelled)


//= ==================END=========================================

app.use(express.static(path.join(__dirname, 'public'))) // DOUBT 4: what is this?

const port = process.env.PORT || config.DEFAULT_PORT
module.exports = app.listen(port, () => {
  mfCommons.log(`Connector listening on port ${port}.`)
})
