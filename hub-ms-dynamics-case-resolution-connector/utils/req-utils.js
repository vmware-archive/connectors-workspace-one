/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/


'use strict'

/**
 * Creates Response Exception object while http request call
 * @param {any} res Express Response
 * @param {any} err error Object
 * @param {any} methodName MethodName In Which The Error Occurs
 * @returns the Error Object
 */
const prepareErrorResponse = (res, err, methodName) => {
  const errorMessage = (err && err.message) || err
  console.trace(`error thrown in prepareErrorResponse is => ${errorMessage}`)

  let error = ''
  try{
    error = errorMessage && JSON.parse(errorMessage)
  }catch (e){
    console.error('Error in parsing error message. errorMessage => '+ errorMessage)
  }
  let status, backendStatus
  if (error && error.statusCode === 401) {
    status = 400
    backendStatus = 401
  } else {
    status = 500
    backendStatus = error.statusCode || 'unknown'
  }

  return res.status(status)
    .header('X-Backend-Status', backendStatus)
    .json({
      method: methodName,
      error: error.message || 'Unknown error'
    })
}

module.exports = {
  prepareErrorResponse
}
