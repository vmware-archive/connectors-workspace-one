/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'

require('dotenv').config()
const express = require('express')

const discovery = require('./routes/discovery')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const zoom = require('./routes/zoom')

const app = express()

app.use(express.json())
app.use(express.urlencoded({ extended: true }))
app.use(express.static('public'))

app.set('trust proxy', true)

const publicKeyUrl = process.env.MF_JWT_PUB_KEY_URI

if (!publicKeyUrl) {
  throw Error('Please provide Mobile Flows public key URL at MF_JWT_PUB_KEY_URI')
}

app.use('/*', mfCommons.handleXRequestId)
app.use('/api/*', mfCommons.validateAuth(publicKeyUrl), mfCommons.readBackendHeaders)

app.get('/health', (req, res) => res.json({ status: 'UP' }))

app.get('/', discovery.root)
app.post('/api/cards', zoom.handleCards)

const port = process.env.PORT || 3000
module.exports = app.listen(port, () => {
  mfCommons.log(`Connector listening on port ${port}.`)
})
