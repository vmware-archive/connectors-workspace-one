/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const service = require('../services/dynamics-case-service')
const reqUtils  = require('../utils/req-utils')
const logger = require('@vmw/mobile-flows-connector-commons')
/**
 * REST Endpoint For Adding Comment to Case
 * @param {any} req request Payload
 * @param {any} res Recieved Hub Payload
 * @return ResponseEntity
 */
const addNoteAboutCase = async (req, res) => {
  try {
    const comment = req.body.comments
    const incidentId = req.body.caseId
    const result = await service.addNotesToCase(res.locals, incidentId, comment)
    logger.log(`addNoteAboutCase for user ${res.locals.mfJwt.email} Status for Adding Notes is ${result}`)
    return res.status(200).json({})
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'addNoteAboutCase')
  }  
}

/**
 * REST Endpoint For Marking Case As Resolved
 * @param {any} req request Payload
 * @param {any} res Recieved Hub Payload
 * @return ResponseEntity
 */
const MarkCaseAsResolved = async (req, res) => {
  try {
    const comment = req.body.comments
    const incidentId = req.body.caseId
    const result = await service.resolveCase(res.locals, incidentId, comment)
    logger.log(`MarkCaseAsResolved for user ${res.locals.mfJwt.email}  Status for Resolving Case is ${result}`)
    return res.status(200).json({})
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'MarkCaseAsResolved')
  }   
}

/**
 * REST Endpoint For Marking Case As Cancelled
 * @param {any} req request Payload
 * @param {any} res Recieved Hub Payload
 * @return ResponseEntity
 */
const MarkCaseAsCancelled = async (req, res) => {
  try {
    const incidentId = req.body.caseId
    const result = await service.cancelCase(res.locals, incidentId)
    logger.log(` MarkCaseAsCancelled for user ${res.locals.mfJwt.email} Status for Canceling Case is ${result}`)
    return res.status(200).json({})
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'MarkCaseAsCancelled')
  }  
}

module.exports = {
  addNoteAboutCase,
  MarkCaseAsResolved,
  MarkCaseAsCancelled
}
