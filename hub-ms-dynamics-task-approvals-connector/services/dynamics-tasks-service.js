/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const axios = require('axios')
const mfCommons = require('@vmw/mobile-flows-connector-commons')

/**
 * prepares the Error 
 * @param {*} err Error PayLoad
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
 * @param {*} locals payLoad Recieved From Hub
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
                        mfCommons.log(`Error While Fetching UserInfo for user => ${locals.mfJwt.email}. Error => ${err.message}`)
                        throw errorObj(err)
                      })
  return resp
 }

/**
 * Fetches the Assigned Tasks
 * @param {*} locals payLoad Recieved From Hub
 * @param {string} userid id of current user
 * @return {Array} Active Tasks Assigned to the CurrentUser
 */
const getMyActiveTasks = async (locals, userid) =>{
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json',
      "Prefer": 'odata.include-annotations=*'
    }
  }
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/api/data/v9.0/tasks?$filter= _ownerid_value eq ${userid} and statecode eq 0 &$select=subject,description,activityid,prioritycode,createdon,scheduledend,statuscode,scheduleddurationminutes,actualdurationminutes,_regardingobjectid_value`, options)
                      .then(r => r.data)
                      .catch(err => {
                        mfCommons.log(`Error While Fetching AssignedTasks for user => ${locals.mfJwt.email}. UserId => ${userid} Error => ${err.message}`)
                        throw errorObj(err)
                      })  
  return resp.value
}

/**
 * Mark the Task As Completed
 * @param {*} locals payLoad Recieved From Hub
 * @param {string} taskId id of Task
 * @return {Number} API Status Code
 */
const markTaskAsCompleted = async(locals,taskId)=>{
    const options = {
        headers: {
          Authorization: locals.connectorAuthorization,
          "Content-Type": "application/json"
        }
      }
      const data = {
        "statecode": 1,
        "statuscode": -1
    }                   
      const resp = await axios
      .patch(`${locals.apiBaseUrl}/api/data/v9.0/tasks(${taskId})`,data, options)
      .then(r => r)
      .catch(err => {
                mfCommons.log(`Error While Completing the Task for user => ${locals.mfJwt.email}. TaskId => ${taskId} Error => ${err.message}`)
                throw errorObj(err)
      })                         
      return resp.status
}

/**
 * Mark the Task As Closed
 * @param {*} locals payLoad Recieved From Hub
 * @param {string} taskId id of Task
 * @return {Number} API Status Code
 */
const closeTask=async(locals,taskId)=>{
    const options = {
        headers: {
          Authorization: locals.connectorAuthorization,
          "Content-Type": "application/json"
        }
      }
      const data = {
          "statecode":2,
          "statuscode":6
        }                  
      const resp = await axios
      .patch(`${locals.apiBaseUrl}/api/data/v9.0/tasks(${taskId})`,data, options)
      .then(r => r)
      .catch(err => {
                mfCommons.log(`Error While Closing the Task for user => ${locals.mfJwt.email}. TaskId => ${taskId} Error => ${err.message}`)
                throw errorObj(err)
      })                         
      return resp.status
}

/**
 * Fetches the Current UserMail From Id
 * @param {*} locals payLoad Recieved From Hub
 * @param {string} userId currentUser Id
 * @returns {string} currentUser Mail
 */
const getCurrentUserMailFromId = async (locals, userId) =>{
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json'
    }
  }
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/api/data/v9.0/systemusers(${userId})?$select=internalemailaddress`, options)
                      .then(r => r.data)
                      .catch(err => {
                        mfCommons.log(`Error While Fetching UserInfo From UserId => ${locals.mfJwt.email}. UserId => ${userId} Error => ${err.message}`)
                        throw errorObj(err)})
  return resp.internalemailaddress
}

module.exports = {
  getUserInfo,
  getMyActiveTasks,
  markTaskAsCompleted,
  getCurrentUserMailFromId,
  closeTask
}
