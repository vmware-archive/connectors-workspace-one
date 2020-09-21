/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

/**
 * Will Convert the Given Object to String
 * 
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
