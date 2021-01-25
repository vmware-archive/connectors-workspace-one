/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const express = require('express')
const utility = require('./utils/utility')
const discovery = require('./routes/discovery')
const request = require('./routes/cards-request')
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
app.use('/api/*', mfCommons.validateAuth(publicKeyUrl), mfCommons.readBackendHeaders, mfCommons.mfRouting.addContextPath, utility.validateReqHeaders)
app.get('/', discovery.root)

app.get('/health', (req, res) => res.json({ status: 'UP' }))

// get new courses from linkedin learning
app.post('/api/cards', request.newCourses)

const port = process.env.PORT || 3000

module.exports = app.listen(port, function () {
  mfCommons.log(`Connector listening on port ${port}.`)
})
