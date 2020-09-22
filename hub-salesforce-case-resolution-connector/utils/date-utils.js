/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

/**
* will convert the given date object into a string in ISO format
* @param {date}  Object
* @returns the string in ISO format
*/
const ISOFormat = (date) => {
  try {
    return date.toISOString()
  } catch (e) {
    console.error(`error in formatting date. date => ${date}`)
  }
}

module.exports = {
  ISOFormat
}
