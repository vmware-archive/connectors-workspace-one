'use strict'

/**
 * @param {any} jsonObj Object
 * @returns the Stringified Object
 */

const stringifyJsonValues = (jsonObj) => {
  if (jsonObj !== null && typeof jsonObj == 'object') {
    Object.entries(jsonObj).forEach(([key, value]) => {
      jsonObj[key] = stringifyJsonValues(value)
    })
  } else {
    return String(jsonObj)
  }
  return jsonObj
}

module.exports = {
  stringifyJsonValues
}
