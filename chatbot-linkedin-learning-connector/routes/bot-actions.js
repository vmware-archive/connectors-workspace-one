'use strict'
const botObjects = require('../utils/bot-objects')

const linkedInService = require('../services/linkedIn-services')
const { log, logReq } = require('../utils/log')

const workflowId = require('../utils/workflow-ids')
const uuid = require('uuid/v4')

/**
 * Show linkedin option catalog to user
 * @param req - Request object
 * @param res - Response object
 * This is not final yet, working with PM to finalize these flows
 */
const optionsCatalog = async (req, res) => {
  let routingPrefix = res.locals.mfRoutingTemplate

  // replace object type with chat bot discovery
  routingPrefix = routingPrefix.replace('INSERT_OBJECT_TYPE', 'botDiscovery')
  const botDiscovery =
    [
      {
        itemDetails: {
          id: uuid,
          title: 'Currently Trending',
          description: 'Get top picks courses for users',
          workflowStep: 'Incomplete',
          workflowId: workflowId.userTopPicks,
          type: 'button',
          actions: [
            {
              title: 'Currently Trending', // Need final string from PM/Manager
              description: 'Get top picks courses for users', // Need final string from PM/Manager
              type: 'GET',
              url: {
                href: `${routingPrefix}bot/actions/top-picks`
              },
              headers: {},
              userInput: []
            }
          ]
        }
      },
      {
        itemDetails: {
          id: uuid,
          title: 'New Courses',
          description: 'Show Options to user',
          workflowStep: 'Incomplete',
          type: 'button',
          workflowId: workflowId.newCourses,
          actions: [
            {
              title: 'Search with Keyword', // Need final string from PM/Manager
              description: 'show user top pick courses', // Need final string from PM/Manager
              type: 'GET',
              url: {
                href: `${routingPrefix}bot/actions/new-courses`
              },
              headers: {},
              userInput: [],
              payload: {}
            }
          ]
        }
      }
    ]
  return res.json({
    objects: botDiscovery
  })
}

/**
 * Fetch new course from linkedin learning
 * @param req - Request object
 * @param res - Response object
 */
const userTopPicks = async (req, res) => {
  logReq(res, 'Sending user top pick course request')
  try {
    const results = await linkedInService.getUserTopPicks(res)
    const courseObjects = botObjects.forBotObjects(results, workflowId.userTopPicks)
    return res.json({ objects: courseObjects })
  } catch (error) {
    return handleLinkedInError(res, error, 'Top picks request failed')
  }
}

const newCourses = async (req, res) => {
  logReq(res, 'Sending new course request')
  try {
    const results = await linkedInService.getNewCourses(res)
    const courseObjects = botObjects.forBotObjects(results, workflowId.newCourses)
    return res.json({ objects: courseObjects })
  } catch (error) {
    return handleLinkedInError(res, error, 'New courses request failed')
  }
}

/**
 * Fetch courses by given keyword search
 * @param req - Request object
 * @param res - Response object
 */
const keywordSearch = async (req, res) => {
  logReq(res, 'Sending keyword search request')
  const keyword = req.body.description || ''
  try {
    const results = await linkedInService.geKeywordSearch(res, keyword)
    const courseObjects = botObjects.forBotObjects(results, workflowId.keywordSearch)
    return res.json({ objects: courseObjects })
  } catch (error) {
    return handleLinkedInError(res, error, 'Keyword search request failed')
  }
}

/*
 * A 401 on the service user cred is mostly because the OAuth token is revoked.
 * @param res - Connector response object.
 * @param error - Error from LinkedIn Learning+ API.
 * @message - Custom message.
 */
const handleLinkedInError = (res, error, message = 'LinkedIn Learning API failed.') => {
  const logMsg = message + ' error: ' + error
  logReq(res, logMsg)

  if (error.statusCode === 401) {
    return res
      .status(400)
      .header('X-Backend-Status', 401)
      .json({ error: error.message })
  } else {
    return res
      .status(500)
      .header('X-Backend-Status', error.statusCode)
      .json({ error: error.message })
  }
}

module.exports = {
  optionsCatalog,
  userTopPicks,
  newCourses,
  keywordSearch
}
