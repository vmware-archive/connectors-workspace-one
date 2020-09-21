/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const service = require('../services/sfdc-task-service')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const reqUtils = require('../utils/req-utils')

/**
 * REST Endpoint For updating the task status
 * @param {any} req request Payload
 * @param {any} res Recieved Hub Payload
 * @return ResponseEntity
 */
const performAction = async (req, res) => {
    try {
      const status = req.body.actionType
      const taskId = req.body.taskId
      const result = await service.updateTaskStatus(res.locals,taskId,status)
      mfCommons.log(`Changing Status to  ${status} for user ${res.locals.mfJwt.email} triggered for task ${taskId}. Response Status=> ${result}`)
      return res.status(200).json({})
    } catch (err) {
      return reqUtils.prepareErrorResponse(res, err, 'performAction')
    }
  }

module.exports = {
performAction
}
