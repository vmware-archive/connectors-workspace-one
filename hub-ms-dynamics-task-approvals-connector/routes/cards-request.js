/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/
'use strict'
const connectorExplang = require('connector-explang')
const btoa = require('btoa')
const crypto = require('crypto');
const service = require('../services/dynamics-tasks-service')
const mfCommons = require('@vmw/mobile-flows-connector-commons')
const commonUtils = require('../utils/common-utils')
const reqUtils = require('../utils/req-utils')
const jsonUtils = require('../utils/json-utils')

/**
 * REST endpoint for generating card objects for Assigned Tasks.
 * @param {*} req request Payload
 * @param {*} res Recieved Hub Payload
 * @return ResponseEntity
 */
const cardsController = async (req, res) => {
  try {
    const userInfo = await service.getUserInfo(res.locals)
    let cardsArray = []
    if (userInfo) {
      const userTasks = await service.getMyActiveTasks(res.locals, userInfo.UserId)
      const assignee = await service.getCurrentUserMailFromId(res.locals,userInfo.UserId)    
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
 * @param {*} locals payLoad Recieved From Hub
 * @param {*} tasks  PayLoad
 * @param {string} assignee current UserMail
 * @return {Array} the Card Objects
 */
const generateCards = (locals, tasks, assignee) => {
  const cardsConfig = require('../config.json')
  delete require.cache[require.resolve('../config.json')]
  const linearizedTasks = linearizeData(locals, tasks, assignee)
  const requestMap = {
    routing_prefix: `${locals.mfRoutingPrefix}action` // used for building action url
  }
  const result = connectorExplang(linearizedTasks, cardsConfig, requestMap)
  mfCommons.log(`linearizeData for ${assignee}  => ${JSON.stringify(linearizeData)}. Generated card from engine => ${result}`)
  return result
}

/**
 * Creates The Linearized Data Required By Engine
 * @param {*} locals payLoad Recieved From Hub
 * @param {*} tasks Tasks PayLoad
 * @return {Array} the Linearize Data
 */
const linearizeData = (locals, tasks, assignee) => {
  return tasks.map(r => {
    const backendId = btoa(`${locals.mfJwt.email}-${r.activityid}-${r.createdon}`)
    const respJson = {
      ':backend_id': backendId,
      ':task-title':r.subject,
      ':regarding':r['_regardingobjectid_value@OData.Community.Display.V1.FormattedValue'],
      ':task-description': r.description,
      ':task-priority': r['prioritycode@OData.Community.Display.V1.FormattedValue'],
      ':task-id': r.activityid,
      ':assignee': assignee,
      ':task-status':r['statuscode@OData.Community.Display.V1.FormattedValue'],
      ':task-due':r.scheduledend
    }
    const filteredKeys= commonUtils.removeEmptyKeys(respJson)
    const hash = crypto.createHash('sha256')
    const card_hash = hash.update(JSON.stringify(filteredKeys)).digest('hex')
    respJson[':card_hash'] = card_hash
    const stringifiedJson = jsonUtils.stringifyJsonValues(filteredKeys)
    return stringifiedJson
  })
}

module.exports = {
  cardsController
}
