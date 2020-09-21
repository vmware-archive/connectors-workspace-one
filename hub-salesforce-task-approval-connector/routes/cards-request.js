/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/
'use strict'
const connectorExplang = require('connector-explang')
const btoa = require('btoa')
const crypto = require('crypto');
const service = require('../services/sfdc-task-service')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const commonUtils = require('../utils/common-utils')
const reqUtils = require('../utils/req-utils')
const jsonUtils = require('../utils/json-utils')

/**
 * REST endpoint for generating card objects for Assigned Tasks.
 * @param {any} req request Payload
 * @param {any} res Recieved Hub Payload
 * @return ResponseEntity
 */
const cardsController = async (req, res) => {
  try {
    const userInfo = await service.getUserInfo(res.locals)
    let cardsArray = []
    if (userInfo) {
      const userTasks = await service.getOpenTasks(res.locals, userInfo.user_id)
      const assignee = userInfo.email    
      if (userTasks && userTasks.length > 0) {
        cardsArray = JSON.parse(generateCards(res.locals, userTasks, assignee))
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
 * Fetches The Card Objects From The Given Tasks PayLoad
 * @param {any} locals payLoad Recieved From Hub
 * @param {any} tasks Tasks PayLoad
 * @param {any} assignee Assigned Task Owner
 * @return {Array} the Card Object
 */
const generateCards = (locals, tasks, assignee) => {
  const cardsConfig = require('../config.json')
  delete require.cache[require.resolve('../config.json')]
  const linearizedTasks = linearizeData(locals, tasks, assignee)
  const requestMap = {
    routing_prefix: `${locals.mfRoutingPrefix}action`
  }
  const result = connectorExplang(linearizedTasks, cardsConfig, requestMap)
  mfCommons.log(`linearizeData for ${assignee}  => ${JSON.stringify(linearizeData)}. Generated card from engine => ${result}`)
  return result
}

/**
 * Creates The Linearized Data Required By Engine
 * @param {any} locals payLoad Recieved From Hub
 * @param {any} tasks Tasks PayLoad
 * @param {any} assignee Assigned Task Owner
 * @return {Array} the Linearized Data
 */
const linearizeData = (locals, tasks, assignee) => {
  return tasks.map(r => {
    const backendId = btoa(`${locals.mfJwt.email}-${r.Id}`)
    const taskLink=`${locals.apiBaseUrl}/${r.Id}`
    const respJson = {
      ':backend_id': backendId,
      ':task-subject': r.Subject,
      ':task-relatedTo': r.account,
      ':task-name': r.contact,
      ':task-description': r.Description,
      ':task-priority': r.Priority,
      ':task-id': r.Id,
      ':assignee': assignee,
      ':task-status':r.Status,
      ':task-due':r.ActivityDate,
      ':task-link': taskLink
    }
    let taskRelated = r.What
    if(taskRelated)
    respJson[':task-account'] = taskRelated.Name
    else 
    respJson[':task-account'] = ''
    let taskName = r.Who
    if(taskName)
    respJson[':task-contact'] = taskName.Name
    else 
    respJson[':task-contact'] = ''
    // Set the card hash to the sha256 of the card body fields
    const hash = crypto.createHash('sha256')
    // Return the final object and remove empty fields
    const filteredKeys = commonUtils.removeEmptyKeys(respJson)
    const card_hash = hash.update(JSON.stringify(filteredKeys)).digest('hex')
    respJson[':card_hash'] = card_hash

    const stringifiedJson = jsonUtils.stringifyJsonValues(filteredKeys)
    return stringifiedJson
  })
}

module.exports = {
  cardsController
}
