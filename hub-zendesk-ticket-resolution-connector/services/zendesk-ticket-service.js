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
  mfCommons.log(`email => ${locals.mfJwt.email}`)
  const resp = await axios.get(`${locals.apiBaseUrl}/api/v2/search.json?query=type:user ${locals.mfJwt.email}`, options)
  .then(r => r.data)
  .catch(err => {
    mfCommons.log(`Error occured in getUserInfo service for user => ${locals.mfJwt.email}. Error => ${err.message}`)
    throw errorObj(err)
  })
  return resp && resp.results && resp.results[0]
}

/**
 * Fetches the userDetails from userId
 * @param {*} locals payLoad Recieved From Hub
 * @param {*} userIds userIds To fetch Data
 * @return {Array} userDetails 
 */
const getUsers = async (locals, userIds) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json'
    }
  }
  return axios.get(`${locals.apiBaseUrl}/api/v2/users/show_many.json?ids=${userIds}`, options)
  .then(resp => resp.data.users)
  .catch(err => {
    mfCommons.log(`Error occured in getUsers service for user => ${locals.mfJwt.email} while fetching data for userIds => ${JSON.stringify(userIds)}. Error => ${err.message}`)
    throw errorObj(err)
  })
}

/**
 * Fetches the user Groups
 * @param {*} locals payLoad Recieved From Hub
 * @param {*} userIds userIds To fetch Data
 * @return {Array} Groups
 */
const getGroups = async (locals) => {
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json'
    }
  }
  return axios.get(`${locals.apiBaseUrl}/api/v2/groups.json`, options)
  .then(resp => resp.data.groups)
  .catch(err => {
    mfCommons.log(`Error occured in getGroups service for user => ${locals.mfJwt.email}. Error => ${err.message}`)
    throw errorObj(err)
  })
}

/**
 * Fetches the Active Tickets of the User
 * @param {*} locals payLoad Recieved From Hub
 * @param {string} assigneeId id of current user
 * @return {Array} Active Tasks Assigned to the CurrentUser
 */
const getUserTickets = async (locals, assigneeId) => {
  // &updated_at>2020-05-28T07:28:01Z -> publish cards which have changed in last two hours except closed ones
  const today = new Date()
  const lastOneHour = new Date(today.getTime() - (1000 * 60 * 60)).toISOString()
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json'
    }
  }
  return axios.get(`${locals.apiBaseUrl}/api/v2/search.json?query=type:ticket assignee_id:${assigneeId} status:open ticket_type:problem`, options)
  .then(resp => resp.data.results)
  .catch(err => {
    mfCommons.log(`Error occured in getUserTickets service for user => ${locals.mfJwt.email} with assignee Id => ${assigneeId}. Error => ${err.message}`)
    throw errorObj(err)
  })
}

/**
 * Fetches the Task Comments
 * @param {*} locals payLoad Recieved From Hub
 * @param {string} ticketId id of Ticket
 * @return {Array} Task Comments
 */
const getTicketComments = async (locals, ticketId) => {
  const options = {
    url: `${locals.apiBaseUrl}/api/v2/tickets/${ticketId}/comments.json`,
    method: 'GET',
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json'
    }
  }
  return axios(options).then(resp => {
    const comments = resp.data.comments
    comments && comments.forEach(comment => {
      comment.ticket_id = ticketId
    })
    return comments
  })
  .catch(err => {
    mfCommons.log(`Error occured in getTicketComments service for user => ${locals.mfJwt.email} for ticketId => ${ticketId}. Error => ${err.message}`)
    throw errorObj(err)
  })
}

/**
 * Adds the Comment to the Ticket
 * @param {*} locals payLoad Recieved From Hub
 * @param {*} ticketId id of the Ticket
 * @param {*} comment comment to be added
 * @returns {Map} Ticket Details
 */
const addTicketComment = async (locals, ticketId, comment) => {
  const data = {
    ticket: {
      comment: comment
    }
  }
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json'
    }
  }
  return axios.put(`${locals.apiBaseUrl}/api/v2/tickets/${ticketId}.json`, data, options)
  .then(resp => resp.data)
  .catch(err => {
    mfCommons.log(`Error occured in addTicketComment service for user => ${locals.mfJwt.email} for ticketId => ${ticketId}. Error => ${err.message}`)
    throw errorObj(err)
  })
}

/**
 * updates the status for the given ticket
 * @param {any} locals payLoad Recieved From Hub
 * @param {string} ticketId id of Ticket
 * @param {string} status status to be changed
 * @param {string} comment comments recieved from hub
 * @return {Map} Ticket Details
 */
const updateTicketStatus = async (locals, ticketId, status, comment) => {
  const data = {
    ticket: {
      status: status,
      comment: comment
    }
  }
  const options = {
    headers: {
      Authorization: locals.connectorAuthorization,
      accept: 'application/json'
    }
  }
  return axios.put(`${locals.apiBaseUrl}/api/v2/tickets/${ticketId}.json`, data, options)
  .then(resp => resp.data)
  .catch(err => {
    mfCommons.log(`Error occured in updateTicketStatus service for user => ${locals.mfJwt.email} for ticketId => ${ticketId}. Error => ${err.message}`)
    throw errorObj(err)
  })
}

module.exports = {
  getUserInfo,
  getUsers,
  getGroups,
  getUserTickets,
  getTicketComments,
  addTicketComment,
  updateTicketStatus
}
