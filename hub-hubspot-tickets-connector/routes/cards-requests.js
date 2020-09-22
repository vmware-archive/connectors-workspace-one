/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const connectorExplang = require('connector-explang')
const btoa = require('btoa')
const crypto = require('crypto');
const service = require('../services/hubspot-services.js')
const logger = require('@vmw/mobile-flows-connector-commons')
const commonUtils = require('../utils/common-utils')
const reqUtils = require('../utils/req-utils')
const jsonUtils = require('../utils/json-utils')

/**
 * REST endpoint for generating card objects for Assigned Tickets.
 * @param {any} req request Payload
 * @param {any} res Recieved Hub Payload
 * @return ResponseEntity
 */
const cardsController = async (req, res) => {
  try {
    const userInfo = await service.getUserInfo(res.locals)
    logger.log(`Recieved Cards Request For HubSpot User With Id => ${userInfo.user}`)
    let cardsArray=[]
    let ticketsArray=[]
    if (userInfo.user) {
      const hubSpotId = await service.getHubSpotIdFromMail(res.locals , userInfo.user)
      const assignedTickets = await service.getAssignedTickets(res.locals, hubSpotId.id)
      const stagesMap = await service.getTicketStagesMap(res.locals)
      const openTickets = assignedTickets.filter(
        k=> (stagesMap[k.properties.hs_pipeline_stage].isClosed != 'true')
      )
      logger.log(`assignedTickets PayLoad => ${JSON.stringify(openTickets)}`)
      if (openTickets && openTickets.length > 0) {
        ticketsArray = await Promise.all (openTickets.map(async r => { 
          if(r.associations && r.associations.companies && r.associations.companies.results.length > 0){  
            let company = await service.getCompanyInfoFromId(res.locals,r.associations.companies.results[0].id)
            r['company'] = company
            } else r['company'] = ''
          if(r.associations && r.associations.contacts && r.associations.contacts.results.length > 0){
            const contacts = await service.getContactNamesOfTicket(res.locals,r.associations.contacts.results.map(r => r.id))
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
      cardsArray = JSON.parse(generateCards(res.locals, ticketsArray, userInfo, stagesMap))
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
 * @param {any} assignedTickets Tickets PayLoad
 * @param {any} userInfo CurrentUser Info
 * @param {any} stagesMap TicketStagesMap
 * @return {Array} the Card Object
 */
const generateCards = (locals, assignedTickets , userInfo, stagesMap) => {
  const cardsConfig = require('../config.json')
  delete require.cache[require.resolve('../config.json')]
  const linearizedTickets = linearizeData(locals, assignedTickets , userInfo, stagesMap)
  const requestMap = {
    routing_prefix: `${locals.mfRoutingPrefix}action` // used for building action url
  }
  const result = connectorExplang(linearizedTickets, cardsConfig, requestMap)
  logger.log(`linearizeData for  => ${JSON.stringify(linearizedTickets)}. Generated card from engine => ${result}`)
  return result
}

/**
 * Creates The Linearized Data Required By Engine
 * @param {any} locals payLoad Recieved From Hub
 * @param {any} assignedTickets Tickets PayLoad
 * @return {Array} the Linearize Data
 */
const linearizeData = (locals, assignedTickets, userInfo, stagesMap) => {
  return assignedTickets.map(r => {
    const backendId = btoa(`${locals.mfJwt.email}-${r.id}`)
    const ticketLink = `https://app.hubspot.com/contacts/${userInfo.hub_id}/ticket/${r.id}`
    const sourceType = r.properties.source_type ? commonUtils.capitalize(r.properties.source_type.toLowerCase()) : ""
    const respJson =  {
      ':backend_id': backendId,
      ':ticket-link':ticketLink,
      ':ticket-id':r.id,
      ':ticket-name':r.properties.subject,
      ':ticket-description': r.properties.content,
      ':ticket-owner-id': r.properties.hubspot_owner_id,
      ':ticket-source-id': userInfo.user,
      ':ticket-status' : stagesMap[r.properties.hs_pipeline_stage].label,
      ':ticket-priority': r.properties.hs_ticket_priority ? commonUtils.capitalize(r.properties.hs_ticket_priority.toLowerCase()) : '',
      ':ticket-created-date': r.createdAt,
      ':ticket-last-customer-reply': r.properties.last_reply_date,
      ':ticket-source-type': sourceType,
      ':ticket-contact-email': r.contact.email,
      ':ticket-contact-phone':r.contact.phone,
      ':ticket-contact-name':r.contact.name,
      ':ticket-company':r.company
    }
    if(r.contacts)
      respJson[':ticket-all-contacts'] = r.contacts.join(', ')
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
