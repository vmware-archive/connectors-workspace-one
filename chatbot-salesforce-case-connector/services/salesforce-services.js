/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

const axios = require('axios')

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
 * @param {*} res payLoad Recieved From Hub
 * @return {Map} current userInfo
 */
const getUserInfo = async (res) => {
  const options = {
    headers: {
      Authorization: res.locals.backendAuthorization,
      accept: 'application/json'
    }
  }
  const resp = await axios
    .get(`${res.locals.backendBaseUrl}/services/oauth2/userinfo`, options)
    .then(r => r.data)
    .catch(err => {
      throw errorObj(err)
    })
  return resp
}

/**
 * Fetches the Active Cases of the User
 * @param {*} res payLoad Recieved From Hub
 * @param {string} userid id of current user
 * @return {Array} Active Cases Assigned to the CurrentUser
 */
const getMyOpenCases = async (res, userid) => {
  const options = {
    headers: {
      Authorization: res.locals.backendAuthorization,
      accept: 'application/json'
    }
  }
  const resp = await axios
    .get(`${res.locals.backendBaseUrl}/services/data/v47.0/query?q=select id,ownerid,contactid,accountid,priority,reason,origin,subject,status,type,casenumber,description,comments,CreatedDate from case where status ='New'  and  OwnerId = '${userid}'`, options)
    .then(r => r.data)
    .catch(err => {
      throw errorObj(err)
    })
  return resp.records
}

module.exports = {
  getUserInfo,
  getMyOpenCases
}
