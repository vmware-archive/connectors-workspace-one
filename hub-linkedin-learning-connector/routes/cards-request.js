'use strict'
const cardObjects = require('../utils/card-objects')

const linkedInService = require('../services/linkedIn-services')
const mfCommons = require('@vmw/mobile-flows-connector-commons')

/**
 * Show linkedin option catalog to user
 * @param req - Request object
 * @param res - Response object
 * This is not final yet, working with PM to finalize these flows
 */
const newCourses = async (req, res) => {
  mfCommons.logReq(res, 'Sending new course request')
  try {
    const results = await linkedInService.getNewCourses(res)
    const courseObjects = cardObjects.forCardObjects(results)
    return res.json({ objects: courseObjects })
  } catch (error) {
    return handleLinkedInError(res, error, 'New courses request failed')
  }
}

/*
 * A 401 on the service user cred is mostly because the OAuth token is revoked.
 * @param res - Connector response object.
 * @param error - Error from LinkedIn Learning+ API.
 * @message - Custom message.
 */
const handleLinkedInError = (res, error, message = 'LinkedIn Learning API failed.') => {
  const logMsg = message + ' error: ' + error
  mfCommons.logReq(res, logMsg)

  if (error.statusCode === 401) {
    return res
      .status(400)
      .header('X-Backend-Status', 401)
      .json({ error: error.message })
  } else {
    return res
      .status(500)
      .header('X-Backend-Status', error.statusCode)
      .json({ error: error.message })
  }
}

module.exports = {
  newCourses
}
