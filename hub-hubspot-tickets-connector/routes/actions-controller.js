/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const service = require('../services/hubspot-services.js')
const logger = require('@vmw/mobile-flows-connector-commons')
const reqUtils = require('../utils/req-utils')

/**
 * REST Endpoint For Performing Actions Like Adding Comment,Logging A Call
 * @param {any} req request Payload
 * @param {any} res Recieved Hub Payload
 * @return ResponseEntity
 */
const performAction = async (req, res) => {
  try {
    const comment = req.body.comments
    const ticketId = req.body.ticketId
    const ownerId = req.body.ownerId
    const sourceId = req.body.sourceId
    const actionType = req.body.actionType
    const timeStamp = Number(new Date())
    const result = await service.performActionOnTicket(res.locals,ticketId,ownerId,sourceId,comment,actionType,timeStamp)
    logger.log(`performing Action ${actionType} for user ${res.locals.mfJwt.email} Status is ${result}`)
    return res.status(200).json({})
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'performAction')
  }  
}

/**
 * REST Endpoint For Updating the Status of the Ticket
 * @param {any} req request Payload
 * @param {any} res Recieved Hub Payload
 * @return ResponseEntity
 */
const updateStatus = async (req, res) => {
    try {
      const ticketId = req.body.ticketId
      const actionType = req.body.actionType
      const result = await service.updateTicketStatus(ticketId, actionType)
      logger.log(`Updating Ticket Status With Id ${ticketId} For user ${res.locals.mfJwt.email} is ${result}`)
      return res.status(200).json({})
    } catch (err) {
      return reqUtils.prepareErrorResponse(res, err, 'updateStatus')
    }  
  }

module.exports = {
  performAction,
  updateStatus
}
