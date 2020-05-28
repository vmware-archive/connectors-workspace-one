'use strict'

let fakeBackend

const startFakeBackend = () => {
  const express = require('express')
  const app = express()
  app.use(express.json())

  app.get('/learningAssets', (req, res) => {
    const requestId = req.headers['x-request-id']
    if (requestId === 'userzero') {
      return res.json(
        require('./linkedin/response/noResultCourse')
      )
    }

    const expDiscovery = require('./linkedin/response/course')
    return res.json(
      expDiscovery
    )
  })
  fakeBackend = app.listen(4000, () => console.log('Fake backend listening on 4000'))
}

const start = () => {
  startFakeBackend()
}

const stop = (fn) => {
  fakeBackend.close(() => {
    fn()
  })
}

module.exports = {
  start,
  stop
}
