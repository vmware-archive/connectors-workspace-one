/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const service = require('../services/dynamics-tasks-service')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const reqUtils = require('../utils/req-utils')

/**
 * REST Endpoint For Completing the Assigned Task
 * @param {*} req request Payload
 * @param {*} res Recieved Hub Payload
 * @return ResponseEntity
 */
const completeTask = async (req, res) => {
  try {
    const activityId = req.body.taskId
    const result = await service.markTaskAsCompleted(res.locals,activityId)
    mfCommons.log(`completeTask for user ${res.locals.mfJwt.email} triggered for activity ${activityId}. Response Status Code => ${result}`)
    return res.status(200).json({})
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'completeTask')
  }
}

/**
 * REST Endpoint For Closing the Assigned Task
 * @param {*} req request Payload
 * @param {*} res Recieved Hub Payload
 * @return ResponseEntity
 */
const cancelTask = async (req, res) => {
  try {
    const  activityId = req.body.taskId
    const result = await service.closeTask(res.locals,activityId)
    mfCommons.log(`cancelTask for user ${res.locals.mfJwt.email} triggered for activity ${activityId}. Response Status Code => ${result}`)
    return res.status(200).json({})
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'cancelTask')
  }
}

module.exports = {
  completeTask,
  cancelTask
}
