/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'
const express = require('express')
const discovery = require('./routes/discovery')
const cardService = require('./routes/card-service')
const documentService = require('./routes/document')


const mfCommons = require('@vmw/mobile-flows-connector-commons')
const app = express()

const mfPublicKeyUrl =  process.env.MF_JWT_PUB_KEY_URI
if (!mfPublicKeyUrl) {
  throw Error('Please provide Mobile Flows public key URL at MF_JWT_PUB_KEY_URI')
}

app.use('/*', mfCommons.handleXRequestId)
app.use(['/api/*'], mfCommons.readBackendHeaders, mfCommons.validateAuth(mfPublicKeyUrl), mfCommons.mfRouting.addContextPath)
app.use(express.json())
app.use(express.urlencoded({ extended: true }))
app.use(express.static('public'))

app.get('/', discovery.root)
app.get('/health', (req, res) => res.json({ status: 'UP' }))
app.post('/api/cards/requests', cardService.getCardObject)
app.get('/api/getDocument/:agreementId', documentService.getDocument)

const port = process.env.PORT || 3000

module.exports = app.listen(port, function () {
  mfCommons.log(`Connector listening on port ${port}.`)
})
