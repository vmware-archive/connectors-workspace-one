/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const axios = require('axios')

/**
 * prepares the Error 
 * @param {*} err Error PayLoad
 * @returns Error 
 */
const errorObj = (err) => {
  if(err.response.status === 404 && err.response.data.message === "The access token is expired or invalid"){
    return new Error(JSON.stringify({
      statusCode: 401,
      message: err.response.data
    }))
  }
 else {
  return new Error(JSON.stringify({
    statusCode: err.response.status,
    message: err.response.data
  }))
  }
}

/**
 * Fetches the current user details
 * @param {any} locals payLoad Recieved From Hub
 * @return {Map} current userInfo
 */
const getUserInfo = async (locals) => {
  const hubToken = `${locals.connectorAuthorization}`
  const token = hubToken.slice(7)
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/oauth/v1/access-tokens/${token}`)
                      .then(r => r.data)
                      .catch(err => {
                                throw errorObj(err)
                      })
  return resp
 }

 /**
  * fetches the HubspotId Using Mail
  * @param {any} locals payLoad Recieved From Hub
  * @param {any} mail current User Mail
  * @returns {Number} the Hubspot Id
  */
 const getHubSpotIdFromMail = async(locals , mail) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization
    }
  }
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/crm/v3/owners`, options)
                      .then(r => r.data)
                      .catch(err => {
                        throw errorObj(err)
                      })
  return resp.results.find( owner => owner.email === mail)
 }

/**
 * Fetches the Assigned Tickets For Current User
 * @param {any} locals payLoad Recieved From Hub
 * @param {string} userid id of current user
 * @return {Array} Active Tickets Assigned to the CurrentUser
 */
const getAssignedTickets = async (locals, hubSpotUserId) =>{
  let allTickets = []
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization
    }
  }
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/crm/v3/objects/tickets?properties=content,subject,createddate,hs_pipeline,hs_pipeline_stage,hs_ticket_priority,last_reply_date,source_type,hubspot_owner_id&associations=Contact,Company&limit=100`, options)
                      .then(r => r.data)
                      .catch(err => {
                        throw errorObj(err)
                      })
  if(resp.paging){
  const firstPageTickets = resp.results
  const remainingTickets= await getPagedTickets(locals,resp.paging.next.link)
  allTickets = allTickets.concat(firstPageTickets).concat(remainingTickets)
  } 
  else { 
  const tickets = resp.results
  allTickets = allTickets.concat(tickets)
  }
  return allTickets.filter(
    m=> m.properties.hubspot_owner_id === hubSpotUserId
    )
}

/**
 * fetches the Paginated Tickets 
 * @param {any} locals payLoad Recieved From Hub
 * @param {any} pagedLink Next PageLink To fetch Tickets 
 * @returns {Array} the Assigned Tickets
 */
const getPagedTickets = async (locals, pagedLink) => {
  let pagedTickets = []
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization
    }
  }
  const resp = await axios
  .get(`${pagedLink}`, options)
  .then(r => r.data)
  .catch(err => {
    throw errorObj(err)
  })
  if(resp.paging){
    const pageTickets = resp.results
    const nextPageTickets = await getPagedTickets(locals,resp.paging.next.link)
    pagedTickets = pagedTickets.concat(pageTickets).concat(nextPageTickets)
    } 
    else { 
    pagedTickets = pagedTickets.concat(resp.results)
    }
    return pagedTickets
}

/**
* Fetches the TicketStagesInfo
* @param {any} locals payLoad Recieved From Hub
* @return {Map} TicketStageMapInfo
*/
const getTicketStagesMap = async (locals) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization
    }
  }
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/crm-pipelines/v1/pipelines/tickets`, options)
                      .then(r => r.data)
                      .catch(err => {
                        throw errorObj(err)
                      })
  const stagesList = resp.results.find(result => result.label === 'Support Pipeline')
  let stagesMap={}
  stagesList.stages.map( stage => 
    stagesMap[stage.stageId] = {"label" : stage.label, "isClosed" : stage.metadata.isClosed}
    )
    return stagesMap
}

/**
* Fetches the Names of contact given by contactIds
* @param {any} locals payLoad Recieved From Hub
* @param {Array} contactIds ids of contact
* @return {Map} contactInfo
*/
const getContactNamesOfTicket = async (locals , contactIds) => {
  const queryParam = contactIds.join('&vid=')
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization
    }
  }
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/contacts/v1/contact/vids/batch?vid=${queryParam}&property=firstname&property=lastname&property=phone&property=email`, options)
                      .then(r => r.data)
                      .catch(err => {
                        throw errorObj(err)
                      })
  
  let contactsMap = Object.values(resp).map( contct => {
    let email = contct.properties.email ? contct.properties.email.value : ""
    let phone = contct.properties.phone ? contct.properties.phone.value : ""
    let firstName = contct.properties.firstname ? contct.properties.firstname.value : ""
    let lastName = contct.properties.lastname ? contct.properties.lastname.value : ""
    let time = contct.properties.lastmodifieddate ? contct.properties.lastmodifieddate.value : ""
    return {'phone':phone,'email':email, 'time':time, 'name':[firstName,lastName].join(' '), 'vid':contct.vid}
  }
    )
    return contactsMap
}

/**
* fetches the Name of Company given by companyId
* @param {any} locals payLoad Recieved From Hub
* @param {string} companyId id of company
* @return {string} Name of the company
*/
const getCompanyInfoFromId = async (locals, companyId) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization
    }
  }
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/companies/v2/companies/${companyId}`, options)
                      .then(r => r.data)
                      .catch(err => {
                        throw errorObj(err)
                      })
  return resp.properties.name.value
}

/**
 *  Will Add Comment Or Log A Call to given Task
 * @param {any} locals payLoad Recieved From Hub
 * @param {any} ticketId Id of the Ticket
 * @param {any} ownerId owner of the Ticket
 * @param {any} sourceId source of the Ticket
 * @param {any} comments comments received from Hub
 * @param {any} actionType action to be performed
 * @returns the API Status
 */
const performActionOnTicket = async (locals, ticketId, ownerId, sourceId, comments, actionType, timeStamp) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json',
      "Content-Type": "application/json"
    }
  }
  const data = {
    "associations": {
      "ownerIds": [],
      "companyIds": [],
      "contactIds": [],
      "dealIds": [],
      "ticketIds": [
        ticketId
      ],
      "engagementsV2UniversalAssociations": {}
    },
    "engagement": {
      "source": "CRM_UI",
      "sourceId": sourceId,
      "type": actionType,
      "timestamp": timeStamp,
      "ownerId": ownerId
    },
    "metadata": {
      "body": `<p>${comments}</p>`
    },
    "attachments": [],
    "scheduledTasks": [],
    "inviteeEmails": []
  }
  const resp = await axios
                      .post(`${locals.apiBaseUrl}/engagements/v1/engagements`,data, options)
                      .then(r => r)
                      .catch(err => {
                                throw errorObj(err)
  })                                      
  return resp.status
}

/**
 * Updates the Status of the Given Ticket
 * @param {*} locals payLoad Recieved From Hub
 * @param {*} ticketId id of the Ticket
 * @param {*} actionType status to be Updated
 * @returns {Number} the API Status Code
 */
const updateTicketStatus = async (locals, ticketId, actionType) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      "Content-Type": "application/json"
    }
  }
  const data = {
    "properties": {
    "hs_pipeline_stage" : actionType
    }
  }
  const resp = await axios
                      .patch(`${locals.apiBaseUrl}/crm/v3/objects/tickets/${ticketId}`,data, options)
                      .then(r => r)
                      .catch(err => {
                                throw errorObj(err)
  })                                      
  return resp.status
}

module.exports = {
  getUserInfo,
  getAssignedTickets,
  getTicketStagesMap,
  performActionOnTicket,
  getContactNamesOfTicket,
  getCompanyInfoFromId,
  getHubSpotIdFromMail,
  updateTicketStatus,
  getPagedTickets
}
