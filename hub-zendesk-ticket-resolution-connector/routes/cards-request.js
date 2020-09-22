/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/
'use strict'
const connectorExplang = require('connector-explang')
const btoa = require('btoa')
const crypto = require('crypto');
const service = require('../services/zendesk-ticket-service')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const commonUtils = require('../utils/common-utils')
const reqUtils = require('../utils/req-utils')
const jsonUtils = require('../utils/json-utils')

/**
 * REST endpoint for generating card objects for Assigned Tickets.
 * @param {*} req request Payload
 * @param {*} res Recieved Hub Payload
 * @return ResponseEntity
 */
const cardsController = async (req, res) => {
  try {
    const userInfo = await service.getUserInfo(res.locals)
    let cardsArray = []
    if (userInfo) {
      const userTickets = await service.getUserTickets(res.locals, userInfo.id)

      const getUniqueIds = (ids) => {
        return ids.reduce((unique, item) => {
          return unique.includes(item) ? unique : [...unique, item]
        }, [])
      }
      const assignee = userInfo.email
      let requesterIds = userTickets.map(ticket => ticket.requester_id)
      requesterIds = getUniqueIds(requesterIds)
      let submitterIds = userTickets.map(ticket => ticket.submitter_id)
      submitterIds = getUniqueIds(submitterIds)
      const userIds = requesterIds.concat(submitterIds)
      const users = await service.getUsers(res.locals, userIds.join())
      const groups = await service.getGroups(res.locals)
      const ticketComments = await Promise.all(userTickets.map(async ticket => await service.getTicketComments(res.locals, ticket.id)))
      if (ticketComments) {
        userTickets.forEach(ticket => {
          ticket.comments = ticketComments.find(el => el.length !== 0 && el[0].ticket_id === ticket.id)
        })
      }
      console.log(`Cards response for user ${assignee} => ${JSON.stringify(userTickets)}`)
      if (userTickets && userTickets.length > 0) {
        cardsArray = JSON.parse(generateCards(res.locals, userTickets, users, groups, assignee))
        mfCommons.log(`Cards response for user ${assignee} => ${JSON.stringify(cardsArray)}`)
        return res.status(200).json(cardsArray)
      }
    }
    return res.status(200).json({ objects: cardsArray })
  } catch (err) {
    return reqUtils.prepareErrorResponse(res, err, 'cardsController')
  }
}

/**
 * Fetches The Card Objects From The Given Tickets PayLoad
 * @param {*} locals payLoad Recieved From Hub
 * @param {*} tickets  Tickets PayLoad
 * @param {*} users  Users MetaData
 * @param {*} groups Groups MetaData
 * @param {*} assignee Task Assignee
 * @return {Array} the Card Objects
 */
const generateCards = (locals, tickets, users, groups, assignee) => {
  const cardsConfig = require('../config.json')
  delete require.cache[require.resolve('../config.json')]
  const linearizedTickets = linearizeData(locals, tickets, users, groups, assignee)
  const requestMap = {
    routing_prefix: `${locals.mfRoutingPrefix}actions` // used for building action url
  }
  const result = connectorExplang(linearizedTickets, cardsConfig, requestMap)
  mfCommons.log(`linearizeData for ${assignee}  => ${JSON.stringify(linearizeData)}. Generated card from engine => ${result}`)
  return result
}

/**
 * Creates The Linearized Data Required By Engine
 * @param {*} locals payLoad Recieved From Hub
 * @param {*} tickets Tickets PayLoad
 * @param {*} users  Users MetaData
 * @param {*} groups Groups MetaData
 * @param {*} assignee Task Assignee
 * @return {Array} the Linearize Data
 */
const linearizeData = (locals, tickets, users, groups, assignee) => {
  /**
   * Linearizes The Input For Comments
   * @param {*} comment comment PayLoad
   * @return {Map} linearized Data
   */
  const constructComment = (comment) => {
    return {
      ':subject': comment.plain_body
    }
  }
  return tickets.map(r => {
    const backendId = btoa(`${locals.mfJwt.email}-${r.id}-${r.created_at}`)
    const groupName = groups && groups.length > 0 ? groups.filter(el => el.id == r.group_id)[0].name : ''
    const requester = users && users.length > 0 ? users.filter(el => el.id == r.requester_id)[0].email : ''
    const submitter = users && users.length > 0 ? users.filter(el => el.id == r.submitter_id)[0].email : ''
    const attachments = r.comments && r.comments && r.comments.flatMap(comment => getAttachmentInfo(comment))
    const tags = r.tags && r.tags.length > 0 ? r.tags.join(', ') : ''
    const filteredCommentsArray = r.comments
    if (filteredCommentsArray && filteredCommentsArray.length > 0) filteredCommentsArray.shift()
    const comments = filteredCommentsArray && filteredCommentsArray.map(comment => constructComment(comment))
    let respJson = {
      ':backend_id': backendId,
      ':ticket_type': r.type,
      ':ticket_description': r.description,
      ':ticket_subject': r.subject,
      ':ticket_priority': r.priority,
      ':ticket_status': r.status,
      ':ticket_raw_subject': r.raw_subject,
      ':ticket_tags': tags,
      ':channel': r.via.channel,
      ':ticket_id': r.id,
      ':ticket_requester': requester,
      ':ticket_submitter': submitter,
      ':ticket_assignee': assignee,
      ':ticket_group': groupName,
      ':comments-section': comments,
      ':attachments-section': attachments
    }
    respJson = commonUtils.removeEmptyKeys(respJson)
    const hash = crypto.createHash('sha256')
    let card_hash = hash.update(JSON.stringify(respJson)).digest('hex')
    respJson[':card_hash'] = card_hash
    const stringifiedJson = jsonUtils.stringifyJsonValues(respJson)
    return stringifiedJson
  })
}

    /**
   * Linearizes The Input For Attachments
   * @param {*} comment comment PayLoad
   * @return {Map} linearized Data
   */
    const getAttachmentInfo = (comment) => {
     const constructAttachment = (attachment) => {
      return {
      ':att_name': attachment.file_name, ':att-link': attachment.content_url
        }
     }
  const items = comment && comment.attachments && comment.attachments.map(el => constructAttachment(el))
  return items
}

module.exports = {
  cardsController
}
