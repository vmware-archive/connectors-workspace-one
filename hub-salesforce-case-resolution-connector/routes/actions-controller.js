/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const service = require('../services/sfdc-case-service')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const reqUtils = require('../utils/req-utils')

/**
 * REST Endpoint For Adding Comment to Case
 * @param {*} req request Payload
 * @param {*} res Recieved Hub Payload
 * @return ResponseEntity
 */
const postComment = async(req,res) => {
  try {
    const comment = req.body.comments
    const caseId = req.body.caseId
    const result = await service.postFeedItemToCase(res.locals, caseId, comment)
    mfCommons.log(`postComment for user ${res.locals.mfJwt.email} Status is ${result}`)
    return res.status(200).json({})
  } 
  catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'postComment')
  }
}

/**
 * REST Endpoint For Changing Case Status
 * @param {*} req request Payload
 * @param {*} res Recieved Hub Payload
 * @return ResponseEntity
 */
const pendingAction = async(req,res) => {
  try {
    const caseId = req.body.caseId
    const actionType=req.body.actionType
      const result = await service.actionsOnCase(res.locals, actionType, caseId)
      mfCommons.log(`Updating Case for user ${res.locals.mfJwt.email} Status is ${result}`)
      return res.status(200).json({})
  } 
  catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'pendingAction')
  }
}

module.exports = {
  postComment,
  pendingAction
}