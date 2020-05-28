
'use strict'

const crypto = require('crypto')
const moment = require('moment')
const uuid = require('uuid/v4')

const zoomRest = require('../services/zoom-rest')
const utility = require('../utils/utility');
const { log, logReq } = require('../utils/log')

const handleCards = async (req, res) => {
  if (!res.locals.baseUrl) {
    return res.status(400).send({ message: 'The x-connector-base-url is required' })
  }

  if (!res.locals.connectorAuthorization) {
    return res.status(400).send({ message: 'The x-connector-authorization is required' })
  }

  logReq(res, 'Sending Zoom recordings request')

  try {
    const recordings = await zoomRest.getMyRecordings(res)

    const cardArray = []
    const baseUrl = utility.derivedBaseUrl(req)

    recordings.forEach(recording => {
      const card = makeCardFromMeetingRecording(req, recording, baseUrl)
      cardArray.push(card)
    })

    return res.status(200).json({ objects: cardArray })
  } catch (error) {
    let status, backendStatus
    if (error.statusCode === 401) {
      status = 400
      backendStatus = 401
    } else {
      status = 500
      backendStatus = error.statusCode || 'unknown'
    }

    return res.status(status)
    .header('X-Backend-Status', backendStatus)
    .json({ method: 'handleCards', error: error.message || 'Unknown error' })
  }
}

const makeCardFromMeetingRecording = (req, recording, baseUrl) => {
  const sha256 = crypto.createHash('sha256').update(recording.uuid, 'utf8')

  const responseCard = {
    id: uuid(),
    backend_id: `${recording.id}`,
    hash: sha256.digest('base64'),
    image: {
      href: 'https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-zoom.png'
    },
    header: {
      title: 'Cloud Recording Now Available',
    },
    body: {
      fields: [
        {
          type: 'GENERAL',
          title: 'Topic',
          description: `${recording.topic}`,
        },
        {
          type: 'GENERAL',
          title: 'Date',
          description: moment(`${recording.start_time}`).format('MMM DD, YYYY LT'),
        },
        {
          type: 'GENERAL',
          title: 'Link to share',
          description: `${recording.share_url}`,
        }
      ]
    },
    actions: [{
      action_key: 'OPEN_IN',
      id: uuid(),
      repeatable: true,
      primary: true,
      label: 'View Recording',
      completed_label: 'View',
      type: 'GET',
      url: {
        href: `${recording.share_url}`
      }
    }],
  }

  return responseCard
}

module.exports = {
  handleCards
}
