/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const connectorExplang = require('connector-explang')
const btoa = require('btoa')
const crypto = require('crypto');
const service = require('../services/sfdc-case-service')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const commonUtils = require('../utils/common-utils')
const reqUtils = require('../utils/req-utils')
const jsonUtils = require('../utils/json-utils')
const dateUtils = require('../utils/date-utils')
/**
 * REST endpoint for generating card objects for Assigned Cases.
 * @param {*} req request Payload
 * @param {*} res Recieved Hub Payload
 * @return ResponseEntity
 */
const cardsController = async (req, res) => {
  try {
    const userInfo = await service.getUserInfo(res.locals)
    let casesArray = []
    let cardsArray = []
    if (userInfo) {
      const userCases = await service.getMyOpenCases(res.locals, userInfo.user_id)
      if (userCases && userCases.length > 0) {
        casesArray = await Promise.all (userCases.map(async caseAssigned => {     
          if(caseAssigned.ContactId){
             const contactInfo =  await service.getContactInfo(res.locals, caseAssigned.ContactId)
             let contact = contactInfo[0].Name 
             caseAssigned['contact'] = contact
          } else {
            caseAssigned['contact'] = ''
          }
          if(caseAssigned.AccountId){
              const accountInfo = await service.getAccountInfo(res.locals, caseAssigned.AccountId)
              let account = accountInfo[0].Name
              caseAssigned['account'] = account
          } else {
            caseAssigned['account'] = ''
          }
           return  caseAssigned
          }  ))
          cardsArray = JSON.parse(generateCards(res.locals, casesArray))
          return res.status(200).json(cardsArray)
      }
    }
    return res.status(200).json({ objects: cardsArray })
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'cardsController')
  }
}

/**
 * Fetches The Card Objects From The Given Cases PayLoad
 * @param {*} locals payLoad Recieved From Hub
 * @param {*} assignedCases Cases PayLoad
 * @return {Array} the Card Objects
 */
const generateCards = (locals, assignedCases) => {
  const cardsConfig = require('../config.json')
  delete require.cache[require.resolve('../config.json')]
  const linearizedCases = linearizeData(locals, assignedCases)
  const requestMap = {
    routing_prefix: `${locals.mfRoutingPrefix}action`
  }
  const result = connectorExplang(linearizedCases, cardsConfig, requestMap)
  mfCommons.log(`linearizeData for  => ${JSON.stringify(linearizeData)}. Generated card from engine => ${result}`)
  return result
}

/**
 * Creates The Linearized Data Required By Engine
 * @param {*} locals payLoad Recieved From Hub
 * @param {*} assignedCases Cases PayLoad
 * @return {Array} the Linearize Data
 */
const linearizeData = (locals, assignedCases) => {
  return assignedCases.map(r => {
    const caseLink=`${locals.baseUrl}/${r.Id}`
    const backendId = btoa(`${locals.mfJwt.email}-${r.Id}-${r.CreatedDate}`)
    let dt = new Date(r.CreatedDate)
    const respJson = {
      ':backend_id': backendId,
      ':case-id':r.Id,
      ':subject': r.Subject,
      ':type': r.Type,
      ':status': r.Status,
      ':account': r.account,
      ':contact': r.contact,
      ':priority': r.Priority,
      ':reason': r.Reason,
      ':description': r.Description,
      ':case-num':r.CaseNumber,
      ':date' :dateUtils.ISOFormat(dt),
      ':case-link':caseLink
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
