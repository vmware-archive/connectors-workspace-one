/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const axios = require('axios');
const mfCommons = require('@vmw/mobile-flows-connector-commons')

/**
 * prepares the Error 
 * @param {*} err Error PayLoad
 * @returns Error 
 */
const errorObj = (err) => {
  if(err.response.status === 403 && (err.response.data === "Bad_OAuth_Token" || err.response.data.toLowerCase() == "missing_oauth_token")){
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
    const options = {
      headers: {
        Authorization: locals.connectorAuthorization,
        accept: 'application/json'
      }
    }
    const resp = await axios
                        .get(`${locals.apiBaseUrl}/services/oauth2/userinfo`, options)
                        .then(r => r.data)
                        .catch(err => {
                                  mfCommons.log(`Error While Fetchng UserInfo  for user => ${locals.mfJwt.email}. Error => ${err.message}`)
                                  throw errorObj(err)
                        })
    return resp || ""
   }

/**
 * Fetches the getOpenTasks of the User 
 * @param {any} locals payLoad Recieved From Hub
 * @param {string} userId id of current user
 * @return {Array} Tasks Assigned to the CurrentUser Which Are Not Completed
 */
const getOpenTasks = async (locals, userId) =>{
    const options = {
      headers: {
        Authorization: locals.connectorAuthorization,
        accept: 'application/json'
      }
    }
    const resp = await axios
                        .get(`${locals.apiBaseUrl}/services/data/v47.0/query?q=select Id,Status, Who.Name,Who.type,What.type,What.Name,subject,activitydate,ownerId,Priority,Description from task where ownerId='${userId}' and Status !='Completed'`, options)
                        .then(r => r.data)
                        .catch(err => {
                                  mfCommons.log(`Error While Fetchng AssignedTasks  for user => ${locals.mfJwt.email}. UserId => ${userId} Error => ${err.message}`)
                                  throw errorObj(err)
                        })
     return resp.records
}

/**
 * Add the Status to the Given Task
 * @param {any} locals payLoad Recieved From Hub
 * @param {string} taskId id of Task
 * @param {string} status status to be Updated
 * @return {Number} API Status Code
 */
const updateTaskStatus = async (locals , taskId, status) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json',
      "Content-Type": "application/json"
    }
  }
  const data = {
  "Status": status
  }
  const resp = await axios
                  .patch(`${locals.apiBaseUrl}/services/data/v47.0/sobjects/Task/${taskId}`,data, options)
                  .then(r => r)
                  .catch(err => {
                                mfCommons.log(`Error While Updating Task Status for user => ${locals.mfJwt.email}. TaskId => ${taskId} Error => ${err.message}`)
                                throw errorObj(err)
                  })    
  return resp.status
}

module.exports = {
  getUserInfo,
  getOpenTasks,
  updateTaskStatus
}
