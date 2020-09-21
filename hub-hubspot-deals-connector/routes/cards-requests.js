/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const connectorExplang = require('connector-explang')
const btoa = require('btoa')
const crypto = require('crypto');
const service = require('../services/hubspot-services.js')
const dateUtils = require('../utils/date-utils')
const  reqUtils = require('../utils/req-utils')
const  logger =  require('@vmw/mobile-flows-connector-commons')
const jsonUtils=  require('../utils/json-utils')
const commonUtils = require('../utils/common-utils')

/**
 * REST endpoint for generating card objects for Assigned Cases.
 * @param {any} req request Payload
 * @param {any} res Recieved Hub Payload
 * @return ResponseEntity
 */
const cardsController = async (req, res) => { // DOUBT 2, Implementations of this function not visible!!!
  try {
    const userInfo = await service.getUserInfo(res.locals)
    logger.log(`Recieved Cards Request For HubSpot User With Id => ${userInfo.user}`)
    let cardsArray=[]
    let dealsArray=[]
    if (userInfo.user) {
      const assignedDeals = await service.getAssignedDeals(res.locals,userInfo.user)
      const stagesMap = await service.getDealStagesMap(res.locals)
      const validDeals = assignedDeals.filter( s => Object.keys(stagesMap).includes(s.properties.dealstage.value))//unexpected Deals Coming Filtering Those
      const openDeals = validDeals.filter(
        k=> (stagesMap[k.properties.dealstage.value].probability != '1.0') && (stagesMap[k.properties.dealstage.value].probability != '0.0')
      )
      logger.log(`assignedDeals PayLoad => ${JSON.stringify(openDeals)}`)
      if (openDeals && openDeals.length > 0) {
        dealsArray = await Promise.all (openDeals.map(async r => { 
          if(r.associations.associatedCompanyIds && r.associations.associatedCompanyIds.length > 0){  
            let company = await service.getCompanyInfoFromId(res.locals,r.associations.associatedCompanyIds[0])
            r['company'] = company
            } else r['company'] = ''
          if(r.associations.associatedVids && r.associations.associatedVids.length > 0){
            const contacts = await service.getContactNamesOfDeal(res.locals,r.associations.associatedVids)
            const contact = contacts.reduce(function(prev, current) {
              return (prev.time > current.time) ? prev : current
          })
            r['contact'] = contact 
            r['contacts']= contacts.map( c=> { return c.email})
          }
          else{
            r['contact'] = ""
            r['contacts']=""
          }
          return r
         }))
      cardsArray = JSON.parse(generateCards(res.locals, dealsArray, userInfo,stagesMap))
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
 * @param {any} assignedDeals Cases PayLoad
 * @return {Array} the Card Object
 */
const generateCards = (locals, assignedDeals , userInfo, stagesMap) => {
  const cardsConfig = require('../config.json')
  delete require.cache[require.resolve('../config.json')]
  const linearizedDeals = linearizeData(locals, assignedDeals , userInfo, stagesMap)
  const requestMap = {
    routing_prefix: `${locals.mfRoutingPrefix}action` // used for building action url
  }
  const result = connectorExplang(linearizedDeals, cardsConfig, requestMap)
  logger.log(`linearizeData for  => ${JSON.stringify(linearizedDeals)}. Generated card from engine => ${result}`)
  return result
}

const dealTypeMap = {
  "existingbusiness":"Existing Business",
  "newbusiness":"New Business",
}

/**
 * Creates The Linearized Data Required By Engine
 * @param {any} locals payLoad Recieved From Hub
 * @param {any} assignedDeals Cases PayLoad
 * @return {Array} the Linearize Data
 */
const linearizeData = (locals, assignedDeals, userInfo, stagesMap) => {
  return assignedDeals.map(r => {
    const backendId = btoa(`${locals.mfJwt.email}-${r.dealId}`)
    const closeDate = r.properties.closedate ? dateUtils.ISOFormat(new Date(Number(r.properties.closedate.value))) : ""
    const lastModifiedDate = r.properties.notes_last_contacted ? dateUtils.ISOFormat(new Date(Number(r.properties.notes_last_contacted.value))) : ""
    const createdDate = r.properties.hs_createdate ? dateUtils.ISOFormat(new Date(Number(r.properties.hs_createdate.value))) : ""
    const amount = r.properties.amount ? r.properties.amount.value : ''
    const daysToClose = r.properties.days_to_close ? r.properties.days_to_close.value : ""
    const dealLink = `https://app.hubspot.com/contacts/${userInfo.hub_id}/deal/${r.dealId}`
    const respJson =  {
      ':backend_id': backendId,
      ':deal-link':dealLink,
      ':deal-id':r.dealId,
      ':deal-name':r.properties.dealname.value,
      ':deal-close-date':closeDate,
      ':deal-owner-id': r.properties.hubspot_owner_id.value,
      ':deal-source-id': r.properties.hubspot_owner_id.sourceId,
      ':deal-last-contacted': lastModifiedDate,
      ':deal-stage': stagesMap[r.properties.dealstage.value].label,
      ':deal-created-date':createdDate,
      ':days-to-close':daysToClose,
      ':deal-type': r.properties.dealtype ? dealTypeMap[r.properties.dealtype.value] : '',
      ':deal-contact-email': r.contact.email,
      ':deal-contact-phone':r.contact.phone,
      ':deal-contact-name':r.contact.name,
      ':deal-company':r.company
    }
    if (amount) 
    respJson[':deal-amount'] = `$ ${amount}`
    if(r.contacts)
    respJson[':deal-all-contacts'] = r.contacts.join(', ')
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
