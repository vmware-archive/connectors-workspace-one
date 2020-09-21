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
                      .get(`${locals.apiBaseUrl}/services/oauth2/userinfo `, options)
                      .then(r => r.data)
                      .catch(err => {
                                mfCommons.log(`Error While Fetchng UserInfo  for user => ${locals.mfJwt.email}. Error => ${err.message}`)
                                throw errorObj(err)
                      })
  return resp || ""
 }

 /**
 * Fetches the Active Cases of the User
 * @param {*} locals payLoad Recieved From Hub
 * @param {string} userid id of current user
 * @return {Array} Active Cases Assigned to the CurrentUser
 */
const getMyOpenCases = async (locals, userid) =>{
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json'
    }
  }
  const resp = await axios
                      .get(`${locals.apiBaseUrl}/services/data/v47.0/query?q=select id,ownerid,contactid,accountid,priority,reason,origin,subject,status,type,casenumber,description,comments,CreatedDate from case where status ='New'  and  OwnerId = '${userid}'`, options)
                      .then(r => r.data)
                      .catch(err => {
                                mfCommons.log(`Error While Fetchng Cases for user => ${locals.mfJwt.email}. UserId => ${userid} Error => ${err.message}`)
                                throw errorObj(err)
                      })      
  return resp.records || ""
}

/**
 * Change the Status of the Case
 * @param {*} locals payLoad Recieved From Hub
 * @param {string} actionType status to be changed
 * @param {string} caseId id of Case
 * @returns {Number} API Status Code
 */
const actionsOnCase = async (locals, actionType, caseId) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json',
      "Content-Type": "application/json"
    }
  }
  const data = {
    "Status": actionType
  }
  const resp = await axios
                        .patch(`${locals.apiBaseUrl}/services/data/v47.0/sobjects/Case/${caseId}`,data, options)
                        .then(r => r)
                        .catch(err => {
                                   mfCommons.log(`Error While Updating Case Status for user => ${locals.mfJwt.email}. caseId => ${caseId} Error => ${err.message}`)
                                   throw errorObj(err)
                         })    
  return resp.status
}

/**
 * fetches the Name of the Contact From ContactId
 * @param {*} locals payLoad Recieved From Hub
 * @param {*} contactId id of the Contact
 * @returns the Contact Information
 */
const getContactInfo = async (locals, contactId) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json'
    }
  }
  const resp = await axios
                       .get(`${locals.apiBaseUrl}/services/data/v47.0/query?q=select name from contact where id='${contactId}'`, options)
                       .then(r => r.data)
                       .catch(err => {
                                 mfCommons.log(`Error While Fetchng ContactInfo for user => ${locals.mfJwt.email}. ContactId => ${contactId} Error => ${err.message}`)
                                 throw errorObj(err)
                        })                                    
  return resp.records || ""
}

/**
 * fetches the Name of the Account From AccountId
 * @param {*} locals payLoad Recieved From Hub
 * @param {*} accountId id of the Account
 * @returns the Account Information
 */
const getAccountInfo = async (locals, accountId) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json'
    }
  }
  const resp = await axios
                       .get(`${locals.apiBaseUrl}/services/data/v47.0/query?q=select name from account where id='${accountId}'`, options)
                       .then(r => r.data)
                       .catch(err => {
                                  mfCommons.log(`Error While Fetchng AccountInfo for user => ${locals.mfJwt.email}. AccountId => ${accountId} Error => ${err.message}`)
                                  throw errorObj(err)
                        })                                   
  return resp.records || ""
}

/**
 * Add the comments to the Given Case
 * @param {*} locals payLoad Recieved From Hub
 * @param {string} caseId id of Case
 * @param {string} comments comments to be added to Case
 * @returns {Number} API Status Code
 */
const postFeedItemToCase = async(locals, caseId, comments) =>{
  const options = {
    headers: {
    Authorization: locals.connectorAuthorization,
    accept:'application/json' ,
    "Content-Type": "application/json" 
  }
}
  const data = {
    "body": {
      "messageSegments": [
        {
          "type": "Text",
          "text": comments
        }
      ]
    },
    "feedElementType": "FeedItem",
    "subjectId": caseId
  }
  const resp= await axios
                      .post(`${locals.apiBaseUrl}/services/data/v47.0/chatter/feed-elements`, data, options)
                      .then(r => r)
                      .catch(err=>{
                                mfCommons.log(`Error While Posting Comment for user => ${locals.mfJwt.email}. caseId => ${caseId} Comments => ${comments} Error => ${err.message}`)
                                throw errorObj(err)
                       })
return resp.status
}

module.exports = {
  getUserInfo,
  getMyOpenCases,
  actionsOnCase,
  getContactInfo,
  getAccountInfo,
  postFeedItemToCase
}
