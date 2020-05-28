
'use strict'

require('dotenv').config()
const express = require('express')

const discovery = require('./routes/discovery')
const auth = require('./utils/auth')
const zoom = require('./routes/zoom')
const utility = require('./utils/utility')
const { log } = require('./utils/log')

const app = express()

app.use(express.json())
app.use(express.urlencoded({ extended: true }))
app.use(express.static('public'))

app.set('trust proxy', true)

app.use('/*', utility.handleXRequestId)
app.use('/api/*', utility.setLocals, auth.validate)

app.get('/health', (req, res) => res.json({status: 'UP'}))

app.get('/', discovery.root)
app.post('/api/cards', zoom.handleCards)

try {
  utility.initConnector()
} catch (e) {
  log('Unable to initialize the connector. \n' + e.message)
  process.exit(1)
}

const port = process.env.PORT || 3000
module.exports = app.listen(port, () => {
  log(`Connector listening on port ${port}.`)
})
