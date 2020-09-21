/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const connectorExplang = require('connector-explang')
const btoa = require('btoa')
const crypto = require('crypto');
const service = require('../services/dynamics-case-service')
const reqUtils = require('../utils/req-utils')
const jsonUtils = require('../utils/json-utils')
const commonUtils = require('../utils/common-utils')
const logger = require('@vmw/mobile-flows-connector-commons')

/**
 * REST endpoint for generating card objects for Assigned Cases.
 * @param {any} req request Payload
 * @param {any} res Recieved Hub Payload
 * @return ResponseEntity
 */
const cardsController = async (req, res) => {
  try {
    const userInfo = await service.getUserInfo(res.locals)
    let cardsArray=[]
    if (userInfo) {
      const activeCases = await service.getActiveCases(res.locals, userInfo.UserId)
      if (activeCases && activeCases.length > 0) {
      cardsArray = JSON.parse(generateCards(res.locals, activeCases))
      return res.status(200).json(cardsArray)
      }
    }
    return res.status(200).json({objects: cardsArray})
} catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'cardsController')
  } 
}

/**
 * Fetches The Card Objects From The Given Cases PayLoad
 * @param {any} locals payLoad Recieved From Hub
 * @param {any} assignedCases Cases PayLoad
 * @return {Array} the Card Object
 */
const generateCards = (locals, assignedCases) => {
  const cardsConfig = require('../config.json')
  delete require.cache[require.resolve('../config.json')]
  const linearizedCases = linearizeData(locals, assignedCases)
  const requestMap = {
    routing_prefix: `${locals.mfRoutingPrefix}action`
  }
  const result = connectorExplang(linearizedCases, cardsConfig, requestMap)
  logger.log(`linearizeData for  => ${JSON.stringify(linearizeData)}. Generated card from engine => ${result}`)
  return result
}

/**
 * Creates The Linearized Data Required By Engine
 * @param {any} locals payLoad Recieved From Hub
 * @param {any} assignedCases Cases PayLoad
 * @return {Array} the Linearize Data
 */
const linearizeData = (locals, assignedCases) => {
  return assignedCases.map(r => {
    const backendId = btoa(`${locals.mfJwt.email}-${r.incidentid}-${r.createdon}`)
    const respJson =  {
      ':backend_id': backendId,
      ':case-title': r.title,
      ':case-description': r.description,
      ':case-customer':  r['_customerid_value@OData.Community.Display.V1.FormattedValue'], 
      ':case-service-stage': r['servicestage@OData.Community.Display.V1.FormattedValue'],
      ':case-subject': r['_subjectid_value@OData.Community.Display.V1.FormattedValue'],
      ':case-contact': r['_primarycontactid_value@OData.Community.Display.V1.FormattedValue'],
      ':case-priority': r['prioritycode@OData.Community.Display.V1.FormattedValue'],
      ':case-status': r['statuscode@OData.Community.Display.V1.FormattedValue'],
      ':case-id': r.ticketnumber,
      ":incident-id":r.incidentid
    }
    const filteredKeys= commonUtils.removeEmptyKeys(respJson)
    const hash = crypto.createHash('sha256')
    const card_hash = hash.update(JSON.stringify(filteredKeys)).digest('hex')
    respJson[':card_hash'] = card_hash
    const stringifiedJson = jsonUtils.stringifyJsonValues(filteredKeys)
    return stringifiedJson
  })
}

module.exports = {
  cardsController  
}
