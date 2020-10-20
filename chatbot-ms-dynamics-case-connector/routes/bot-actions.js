/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

'use strict'
const botObjects = require('../utils/bot-objects')
const mfCommons = require('@vmw/mobile-flows-connector-commons')

const dynamicCasesService = require('../services/dynamics-services')

const workflowId = require('../utils/workflow-ids')

const pendingCases = async (req, res) => {
  mfCommons.logReq(res, 'cases pending request')
  try {
    const userInfo = await dynamicCasesService.getUserInfo(res)
    const results = await dynamicCasesService.getActiveCases(res, userInfo.UserId)
    const cases = results.map(l => {
      l.url = `${res.locals.backendBaseUrl}/main.aspx?pagetype=entityrecord&etn=incident&id=${l.incidentid}`
      return l
    })
    const casesObjects = botObjects.forBotObjects(cases, workflowId.pendingCases)
    return res.json({ objects: casesObjects })
  } catch (error) {
    return handleError(res, error)
  }
}

const handleError = (res, error, message = 'MS Dynamics API failed.') => {
  const logMsg = message + ' error: ' + error
  mfCommons.logReq(res, logMsg)

  if (error.statusCode === 401) {
    return res
      .status(400)
      .header('X-Backend-Status', 401)
      .json({ error: error.message })
  } else {
    return res
      .status(500)
      .header('X-Backend-Status', error.statusCode)
      .json({ error: error.message })
  }
}

module.exports = {
  pendingCases
}
