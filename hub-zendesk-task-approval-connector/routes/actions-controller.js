/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const service = require('../services/zendesk-tasks-service')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const reqUtils = require('../utils/req-utils')

/**
 * REST Endpoint For Approving the Request
 * @param {*} req request Payload
 * @param {*} res Recieved Hub Payload
 * @return ResponseEntity
 */
const approveRequest = async (req, res) => {
  try {
    const status = req.body.actionType
    const ticketId = parseInt(req.body.ticketId)
    const comment = req.body.comments
    const result = await service.updateTicketStatus(res.locals, ticketId, status, comment)
    const tiketId = result.audit.ticket_id
    mfCommons.log(`approveRequest for user ${res.locals.mfJwt.email} triggered for ticket ${ticketId}. Response => ${JSON.stringify(result)}`)
    return res.status(200).json({ tiketId })
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'approveRequest')
  }
}

/**
 * REST Endpoint For Declining the Request
 * @param {*} req request Payload
 * @param {*} res Recieved Hub Payload
 * @return ResponseEntity
 */
const declineRequest = async (req, res) => {
  try {
    const status = req.body.actionType
    const ticketId = parseInt(req.body.ticketId)
    const comment = req.body.comments
    const result = await service.updateTicketStatus(res.locals, ticketId, status, comment)
    const tiketId = result.audit.ticket_id
    mfCommons.log(`declineRequest for user ${res.locals.mfJwt.email} triggered for ticket ${ticketId}. Response => ${JSON.stringify(result)}`)
    return res.status(200).json({ tiketId })
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'declineRequest')
  }
}

module.exports = {
  approveRequest,
  declineRequest
}
