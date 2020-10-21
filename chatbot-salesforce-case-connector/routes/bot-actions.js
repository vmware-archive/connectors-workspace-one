/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'
const botObjects = require('../utils/bot-objects')

const service = require('../services/salesforce-services')
const mfCommons = require('@vmw/mobile-flows-connector-commons')


const workflowId = require('../utils/workflow-ids')
const { v4: uuid } = require('uuid')

const getPendingCases = async (req, res) => {
  mfCommons.logReq(res, 'Fetching Cases request')
  try {
    const userInfo = await service.getUserInfo(res)
    const userCases = await service.getMyOpenCases(res, userInfo.user_id)
    const userCasesInfo = userCases.map(l => {
      l['link'] = `${res.locals.backendBaseUrl}/${l.Id}`
      return l
    })
    const caseObjects = botObjects.forBotObjects(userCasesInfo, workflowId.pendingCases)
    return res.json({ objects: caseObjects })
  } catch (error) {
    return prepareErrorResponse(res, error, 'New courses request failed')
  }
}

const prepareErrorResponse = (res, err, errorText) => {
  const errorMessage = (err && err.message) || err
  mfCommons.logReq(res, `error thrown in prepareErrorResponse is => ${errorMessage}`)
  let error = ''
  try{
    error = errorMessage && JSON.parse(errorMessage)
  }catch {
    mfCommons.logReq(res, `Error in parsing error message. errorMessage => ${errorMessage}`)
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
      method: errorText,
      error: error.message || 'Unknown error'
    })
}

module.exports = {
 getPendingCases
}
