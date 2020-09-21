/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const axios = require('axios')
const dateUtils = require('../utils/date-utils')
const mfCommons = require('@vmw/mobile-flows-connector-commons')


/**
 * prepares the Error 
  @param {} err Error PayLoad
 * @returns Error 
 */
const errorObj = (err) => {
  return new Error(JSON.stringify({
    statusCode: err.response.status,
    message: err.response.data
  }))
}

/**
 * Fetches the current user details
 * @param {any} locals payLoad Recieved From Hub
 * @return {Map} current userInfo
 */
const getUserInfo = async (locals) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json'
    }
  }
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/api/data/v9.0/WhoAmI`, options)
                      .then(r => r.data)
                      .catch(err => {
                        mfCommons.log(`Error While Fetchng UserInfo  for user => ${locals.mfJwt.email}. Error => ${err.message}`)
                                throw errorObj(err)
                      })
  return resp || ""
 }

/**
 * Fetches the Active Cases of the User In Last One Hour
 * @param {any} locals payLoad Recieved From Hub
 * @param {string} userid id of current user
 * @return {Array} Active Cases Assigned to the CurrentUser
 */
const getActiveCases = async (locals, userid) =>{
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json',
      'Prefer':'odata.include-annotations=*'
    }
  }
  var oneHoursBefore = new Date()
  oneHoursBefore.setHours(oneHoursBefore.getHours() - 1)
  let lookupDate = dateUtils.ISOFormat(oneHoursBefore)
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/api/data/v9.0/incidents?$filter= _ownerid_value eq ${userid} and statecode eq 0 and createdon gt ${lookupDate}`, options)
                      .then(r => r.data)
                      .catch(err => {
                        mfCommons.log(`Error While Fetchng Cases for user => ${locals.mfJwt.email}. UserId => ${userid} Error => ${err.message}`)
                        throw errorObj(err)
                      })                 
  return resp.value || ""
}

/**
 * Add the comments to the Given Case
 * @param {any} locals payLoad Recieved From Hub
 * @param {string} incidentId id of Case
 * @param {string} comments comments to be added to Case
 * @return {Number} API Status Code
 */
const addNotesToCase = async (locals, incidentId, comments) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json',
      "Content-Type": "application/json"
    }
  }
  const data = {
    "notetext": comments,
    "objectid_incident@odata.bind": `incidents(${incidentId})`
  }
  const resp = await axios
                      .post(`${locals.apiBaseUrl}/api/data/v9.0/annotations`,data, options)
                      .then(r => r)
                      .catch(err => {
                        mfCommons.log(`Error While Adding Notes to Case for user => ${locals.mfJwt.email}. CaseId => ${incidentId} Error => ${err.message}`)
                                throw errorObj(err)
                       })                                      
  return resp.status
}

/**
 * Mark the Case As Resolved
 * @param {any} locals payLoad Recieved From Hub
 * @param {string} incidentId id of Case
 * @param {string} comments comments to be added to Case
 * @return {Number} API Status Code
 */
const resolveCase = async(locals, incidentId, comments) => {
  const options = {
    headers: {
    Authorization: locals.connectorAuthorization,
    accept:'application/json' ,
    "Content-Type": "application/json" 
  }
}
const data={
      "IncidentId": {
      "incidentid": incidentId,
      "@odata.type": "Microsoft.Dynamics.CRM.incident"
    },
    "Status": 5,
    "BillableTime": 60,
    "Resolution": comments,
    "Remarks": ""
  }
const resp= await axios
                        .post(`${locals.apiBaseUrl}/api/data/v9.0/ResolveIncident?tag=abortbpf`, data,options)
                        .then(r => r)
                        .catch(err=>{
                          mfCommons.log(`Error While Resolving Case for user => ${locals.mfJwt.email}. CaseId => ${incidentId} Error => ${err.message}`)
                          throw errorObj(err)
                        })               
return resp.status
}

/**
 * Mark the Case As Cancelled
 * @param {any} locals payLoad Recieved From Hub
 * @param {string} incidentId id of Case
 * @return {Number} API Status Code
 */
const cancelCase = async (locals, incidentId) =>{
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept :'application/json',
      "Content-Type": "application/json"
    } 
  }
  const data={
    "statecode": 2,
    "statuscode": -1
  }
  const resp= await axios
                        .patch(`${locals.apiBaseUrl}/api/data/v9.0/incidents(${incidentId})`, data, options)
                        .then(r => r)
                        .catch(err=>{
                          mfCommons.log(`Error While Canceling Case for user => ${locals.mfJwt.email}. CaseId => ${incidentId} Error => ${err.message}`)
                          throw errorObj(err)
                        })          
  return resp.status
}

module.exports = {
  getUserInfo,
  getActiveCases,
  addNotesToCase,
  resolveCase,
  cancelCase   
}
