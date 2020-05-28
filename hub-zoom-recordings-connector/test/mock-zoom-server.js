'use strict'

let fakeBackend

const startFakeBackend = () => {
  const express = require('express')
  const recordings = require('./zoom-recordings')

  const app = express()
  app.use(express.json())

  app.get('/users/me/recordings', (req, res) => {
    return res.json(recordings)
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
