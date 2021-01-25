'use strict'

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
  validateReqHeaders
}
