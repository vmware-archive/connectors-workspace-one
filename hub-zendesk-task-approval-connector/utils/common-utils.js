'use strict'

/**
 * It Will set required parameters For Connector.
 *
 * @param  req - Request object
 * @param  res - Response object
 * @param  next - Express next function.
 */
const removeEmptyKeys = obj => {
  Object.entries(obj).forEach(([key, val]) =>
    ((val && typeof val === 'object') && removeEmptyKeys(val)) || ((val === null || val === '') && delete obj[key])
  )
  return obj
}

module.exports = {
  removeEmptyKeys
}
