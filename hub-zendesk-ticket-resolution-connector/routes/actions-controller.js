/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const service = require('../services/zendesk-ticket-service')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const reqUtils = require('../utils/req-utils')

/**
 * REST Endpoint For Adding Comment to the Ticket
 * @param {*} req request Payload
 * @param {*} res Recieved Hub Payload
 * @return ResponseEntity
 */
const addTicketComment = async (req, res) => {
  try {
    const comment = req.body.comments
    const ticketId = parseInt(req.body.ticketId)
    const result = await service.addTicketComment(res.locals, ticketId, comment)
    const commentId = result.audit.events[0].id
    mfCommons.log(`addTicketComment for user ${res.locals.mfJwt.email} triggered for ticket ${ticketId}. Response => ${JSON.stringify(result)}`)
    return res.status(200).json({ commentId })
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'addTicketComment')
  }
}

/**
 * REST Endpoint For Updating the Status of the Ticket
 * @param {*} req request Payload
 * @param {*} res Recieved Hub Payload
 * @return ResponseEntity
 */
const updateTicketStatus = async (req, res) => {
  try {
    const status = req.body.actionType
    const ticketId = parseInt(req.body.ticketId)
    const comment = req.body.comments
    const result = await service.updateTicketStatus(res.locals, ticketId, status, comment)
    const tiketId = result.audit.ticket_id
    mfCommons.log(`updateTicketStatus for user ${res.locals.mfJwt.email} triggered for ticket ${ticketId}. Response => ${JSON.stringify(result)}`)
    return res.status(200).json({tiketId})
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'updateTicketStatus')
  }
}

module.exports = {
  addTicketComment,
  updateTicketStatus
}
