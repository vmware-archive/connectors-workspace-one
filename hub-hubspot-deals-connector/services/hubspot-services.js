/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const axios = require('axios')


/**
 * prepares the Error 
  @param {} err Error PayLoad
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
 * Fetches the Assigned Deals of the User In Last One Hour
 * @param {any} locals payLoad Recieved From Hub
 * @param {string} userid id of current user
 * @return {Array} Active Cases Assigned to the CurrentUser
 */
const getAssignedDeals = async (locals, userId) =>{
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization
    }
  }
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/deals/v1/deal/paged?includeAssociations=true&properties=dealname&properties=hubspot_owner_id&properties=closedate&properties=dealstage&properties=amount&properties=days_to_close&properties=notes_last_contacted&properties=hs_createdate&properties=dealtype`, options)
                      .then(r => r.data)
                      .catch(err => {
                        throw errorObj(err)
                      })                 
  const deals = resp.deals
  return deals.filter(
    m=> m.properties.hubspot_owner_id.sourceId === userId
    )
}

/**
 * Fetches the DealStagesInfo
 * @param {any} locals payLoad Recieved From Hub
 * @return {Map} DealStageMapInfo
 */
const getDealStagesMap = async (locals) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization
    }
  }
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/crm-pipelines/v1/pipelines/deals`, options)
                      .then(r => r.data)
                      .catch(err => {
                        throw errorObj(err)
                      })
  const stagesList = resp.results.find(result => result.label === 'Sales Pipeline')
  let stagesMap={}
  stagesList.stages.map( stage => 
    stagesMap[stage.stageId] = {"label" : stage.label, "probability" : stage.metadata.probability}
    )
    return stagesMap
}

/**
 * Fetches the Names of contact given by contactIds
 * @param {any} locals payLoad Recieved From Hub
 * @param {Array} contactIds ids of contact
 * @return {Map} contactInfo
 */
const getContactNamesOfDeal = async (locals , contactIds) => {
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
 * Fetches Company Info given by companyId
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
 *  Will Add Comment Or Log A Call to given Deal
 * @param {any} locals payLoad Recieved From Hub
 * @param {any} dealId Id of the Deal
 * @param {any} ownerId owner of the Deal
 * @param {any} sourceId source of the Deal
 * @param {any} comments comments received from Hub
 * @param {any} actionType action to be performed
 * @param {any} timeStamp Current Time Stamp
 * @returns the API Status
 */
const performActionOnDeal = async (locals, dealId, ownerId, sourceId, comments, actionType, timeStamp) => {
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
      "dealIds": [dealId],
      "ticketIds": [],
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

module.exports = {
  getUserInfo,
  getAssignedDeals,
  getDealStagesMap,
  performActionOnDeal,
  getContactNamesOfDeal,
  getCompanyInfoFromId
}
