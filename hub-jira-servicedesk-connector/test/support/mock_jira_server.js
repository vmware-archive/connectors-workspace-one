const express = require('express')
const bodyParser = require('body-parser')
const servicedesks = require('./files/list_service_desks')
const accessibleresources = require('./files/get_accessible_resources')
const requesttypes = require('./files/get_request_types')
const requests = require('./files/get_customer_requests')
const approval = require('./files/get_approval')
const approvalresponse = require('./files/approval_response')
const createRequest = require('./files/create_customer_request')
/**
 * Create a Mock Jira Service Desk service with the appropriate APIs to respond to the connector
 */
const createServer = () => {
  const port = process.env.MOCK_JIRA_SERVER_PORT || 10001
  const app = express()
  app.use(bodyParser.json())
  const baseURL = `http://localhost:${port}`

  app.get('/servicedesk', function (req, res) {
    res.status(200).json(servicedesks(baseURL))
  })

  app.get('/servicedesk/:serviceDeskId/requesttype', function (req, res) {
    const serviceDeskId = req.params.serviceDeskId
    res.status(200).json(requesttypes(baseURL, serviceDeskId))
  })

  app.get('/oauth/token/accessible-resources', function (req, res) {
    res.status(200).json(accessibleresources())
  })

  app.get('/request', function (req, res) {
    // ?requestOwnership=APPROVER&requestStatus=OPEN_REQUESTS&approvalStatus=MY_PENDING_APPROVAL&expand=requestType
    res.status(200).json(requests(baseURL))
  })

  app.get('/request/:approvalIssueId/approval', function (req, res) {
    const approvalIssueId = req.params.approvalIssueId
    res.status(200).json(approval(baseURL, approvalIssueId))
  })

  app.post('/request/:approvalIssueId/approval/:approvalId', function (req, res) {
    const approvalIssueId = req.params.approvalIssueId
    const approvalId = req.params.approvalId
    const decision = req.body.decision
    res.status(200).json(approvalresponse(baseURL, approvalIssueId, approvalId, decision))
  })

  app.post('/request', (req, res) => {
    const summary = req.body.requestFieldValues.summary
    const description = req.body.requestFieldValues.description
    res.status(200).json(createRequest(baseURL, summary, description))
  })

  function errorHandler (err, req, res, next) {
    console.log('ERROR')
    if (res.headersSent) {
      return next(err)
    }
    res.status(500).json({ error: err })
  }
  app.use(errorHandler)

  const server = app.listen(port)
  console.log(`*** Mock jira is listening (${port}), call close() when finished`)

  app.close = function (fn) {
    console.log('*** Mock jira is shutting down')
    server.close(() => fn())
  }

  return app
}

module.exports = {
  createServer
}
