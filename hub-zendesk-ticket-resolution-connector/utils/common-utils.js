'use strict'

/**
 * will remove the null,undefined and empty values in the given object
 * @param {any} obj Object
 * @returns the filtered Object
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
