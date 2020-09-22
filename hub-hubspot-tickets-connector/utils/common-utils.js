/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

/**
 * will remove the null,undefined and empty values in the given object
 * @param {any} obj Object
 * @returns the filtered Object
 */
const removeEmptyKeys = obj => {
  Object.entries(obj).forEach(([key, val]) =>
    ((val && typeof val === 'object') && removeEmptyKeys(val)) || ((val === null || val === '' || val === undefined) && delete obj[key])
  )
  return obj
}

/**
 * Will capitalize the Given String
 * For COMPANY => Company
 * For company -> Company
 * 
  @param {} s String
 */
const capitalize = (s) => {
  if (typeof s !== 'string') return ''
  return s.charAt(0).toUpperCase() + s.slice(1)
}


module.exports = {
  removeEmptyKeys,
  capitalize
}
