/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const service = require('../services/hubspot-services.js')
const  logger  = require('@vmw/mobile-flows-connector-commons')
const reqUtils  = require('../utils/req-utils')

/**
 * REST Endpoint For Performing Actions Like Adding Comment,Logging A Call
 * @param {any} req request Payload
 * @param {any} res Recieved Hub Payload
 * @return ResponseEntity
 */
const performAction = async (req, res) => {
  try {
    const comment = req.body.comments
    const dealId = req.body.dealId
    const ownerId = req.body.ownerId
    const sourceId = req.body.sourceId
    const actionType = req.body.actionType
    const timeStamp = Number(new Date())
    const result = await service.performActionOnDeal(res.locals,dealId,ownerId,sourceId,comment,actionType,timeStamp)
    logger.log(`performing Action ${actionType} for user ${res.locals.mfJwt.email} Status for Adding Notes is ${result}`)
    return res.status(200).json({})
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'performAction')
  }  
}

module.exports = {
  performAction
}
