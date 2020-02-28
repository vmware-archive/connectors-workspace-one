/**
 * All exported methods should take a request and response and should be declared async
 * All methods should be handling the response directly within them and
 * should not expect to return a Promise
 */
const jiraRest = require('./jira_rest')
const jiraUtil = require('./jira_util')

var hash = 'create_card'
/**
 * Set the backend_id and update the hash for the create card, to allow for updates and forcing a new card
 * @param  {} newHash
 */
const setHash = (newHash) => {
  if (newHash) {
    hash = newHash
  }
}
/**
 * The published card request endpoint
 * @param  {} req
 * @param  {} res
 */
const handleCards = async (req, res) => {
  try {
    const connectorAuthorization = res.locals.connectorAuthorization
    const cloudId = res.locals.cloudId
    const customerRequests = await jiraRest.getCustomerRequestsPendingApproval(connectorAuthorization, cloudId)

    const cardArray = []
    req.hash = hash
    cardArray.push(jiraUtil.makeStaticTicketCreationCard(req)) // static card, temporary

    customerRequests.forEach(customerRequest => {
      const card = jiraUtil.makeCardFromCustomerRequest(req, customerRequest)
      cardArray.push(card)
    })

    const responseJSON = {
      objects: cardArray
    }
    if (process.env.DEBUG) {
      console.log(`Sending status 200 and cardArray with ${cardArray.length} cards`)
    }
    res.status(200).json(responseJSON)
  } catch (error) {
    console.log(error.message || 'Unknown error')
    if (error.statusCode) {
      res.header('X-Backend-Status', [error.statusCode])
    }
    res.status(400).json({ method: 'handleCards', 'backend-status': error.statusCode || 'unknown' })
  }
}
/**
 * The published service desk list endpoint
 * @param  {} req
 * @param  {} res
 */
const handleListServiceDesks = async (req, res) => {
  try {
    const connectorAuthorization = req.header('x-connector-authorization')
    const cloudId = res.locals.cloudId
    const serviceDesks = await jiraRest.listServiceDesks(connectorAuthorization, cloudId)

    if (process.env.DEBUG) {
      console.log(`Sending status 200 and action with ${serviceDesks.length} service desks`)
    }
    res.status(200).json(serviceDesks)
  } catch (error) {
    if (process.env.DEBUG) {
      console.log(error.message || 'Unknown error')
    }
    if (error.statusCode) {
      res.header('X-Backend-Status', [error.statusCode])
    }
    res.status(400).json({ method: 'handleListServiceDesks', 'backend-status': error.statusCode || 'unknown' })
  }
}
/**
 * The published request type list endpoint
 * @param  {} req
 * @param  {} res
 */
const handleListRequestTypes = async (req, res) => {
  try {
    const connectorAuthorization = req.header('x-connector-authorization')
    const cloudId = res.locals.cloudId
    const serviceDeskId = req.body.serviceDeskId || 1
    const requestTypes = await jiraRest.listRequestTypes(serviceDeskId, connectorAuthorization, cloudId)

    if (process.env.DEBUG) {
      console.log(`Sending status 200 and action with ${requestTypes.length} request types`)
    }
    res.status(200).json(requestTypes)
  } catch (error) {
    if (process.env.DEBUG) {
      console.log(error.message || 'Unknown error')
    }
    if (error.statusCode) {
      res.header('X-Backend-Status', [error.statusCode])
    }
    res.status(400).json({ method: 'handleListRequestTypes', 'backend-status': error.statusCode || 'unknown' })
  }
}

/**
 * The published approval request endpoint
 * @param  {} req
 * @param  {} res
 */
const handleApprovalAction = async (req, res) => {
  try {
    const connectorAuthorization = req.header('x-connector-authorization')
    const cloudId = res.locals.cloudId
    const issueKey = req.body.issueKey
    const decision = req.body.decision
    const comment = req.body.comment
    if (process.env.DEBUG) {
      console.log(`${issueKey} -- ${decision}`)
    }

    const approval = await jiraRest.getApprovalDetail(issueKey, connectorAuthorization, cloudId)

    if (!approval) {
      res.status(400).json({
        error: 'no approval found'
      })
      return
    }

    if (comment) {
      const commentResult = await jiraRest.postCommentOnRequest(issueKey, comment, connectorAuthorization, cloudId)
      if (process.env.DEBUG) {
        console.log(JSON.stringify(commentResult))
      }
    }

    const result = await jiraRest.approveOrDenyApproval(decision, issueKey, approval.id, connectorAuthorization, cloudId)

    if (process.env.DEBUG) {
      console.log(`Sending status 200 and action with ${result} result`)
    }

    res.status(200).json({
      status: result
    })
  } catch (error) {
    if (process.env.DEBUG) {
      console.log(error.message || 'Unknown error')
    }
    if (error.statusCode) {
      res.header('X-Backend-Status', [error.statusCode])
    }
    res.status(400).json({ method: 'handleApprovalAction', 'backend-status': error.statusCode || 'unknown' })
  }
}
/**
 * The published endpoint to create customer requests
 * Note: For now, assume serviceDesk ID 1 and requestType ID 1 if not specified
 * @param  {} req
 * @param  {} res
 */
const handleCreateCustomerRequest = async (req, res) => {
  try {
    const connectorAuthorization = req.header('x-connector-authorization')
    const cloudId = res.locals.cloudId
    const serviceDeskId = req.body.serviceDeskId || 1
    const requestTypeId = req.body.requestTypeId || 1
    const summary = req.body.summary || 'Request for Assistance'
    const description = req.body.description || 'Default description here'
    if (!serviceDeskId || !requestTypeId || !summary || !description) {
      res.status(400).json({ error: 'Missing required parameters (serviceDeskId, requestTypeId, summary, description)' })
      return
    }

    const result = await jiraRest.createCustomerRequest(serviceDeskId, requestTypeId, summary, description, connectorAuthorization, cloudId)
    const success = { issueId: result.issueId, issueKey: result.issueKey }
    res.status(200).json(success)
  } catch (error) {
    if (process.env.DEBUG) {
      console.log(error.message || 'Unknown error')
    }
    if (error.statusCode) {
      res.header('X-Backend-Status', [error.statusCode])
    }
    res.status(400).json({ method: 'handleCreateCustomerRequest', 'backend-status': error.statusCode || 'unknown' })
  }
}
/**
 * Add the Jira Service Desk Cloud ID to the res.locals for future middleware to use
 * @param  {} req
 * @param  {} res
 * @param  {} next
 */
const getCloudId = async (req, res, next) => {
  if (res.locals.cloudId) {
    next()
  } else {
    const cloudId = await jiraRest.getCloudId(res.locals.connectorAuthorization)
    if (cloudId) {
      res.locals.cloudId = cloudId
      next()
    } else {
      res.status(500).json({ error: 'Could not find a valid cloud ID for this account' })
    }
  }
}

module.exports = {
  setHash,
  getCloudId,
  handleCards,
  handleApprovalAction,
  handleCreateCustomerRequest,
  handleListServiceDesks,
  handleListRequestTypes
}
