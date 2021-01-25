'use strict'

const LINKEDIN_LOGO_PATH = 'https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-linkedin-learning.png'

const validateReqHeaders = (req, res, next) => {
  if (!res.locals.backendBaseUrl) {
    return res.status(400).send({ message: 'The x-connector-base-url is required' })
  }

  if (!res.locals.backendAuthorization) {
    return res.status(400).send({ message: 'The x-connector-authorization is required' })
  }
  next()
}

module.exports = {
  validateReqHeaders,
  LINKEDIN_LOGO_PATH
}
