/**
 * This file contains outbound REST requests to the Jira Service Desk APIs
 * All exposed methods should be delcared as async and should return a Promise
 */
const rp = require('request-promise-native')
require('dotenv').config()

const ATLASSIAN_API_SERVER = process.env.ATLASSIAN_API_SERVER || 'https://api.atlassian.com'

const getServiceDeskRequestAPI = (cloudId) => {
  const url = process.env.SERVICEDESK_REQUEST_API || `${ATLASSIAN_API_SERVER}/ex/jira/${cloudId}/rest/servicedeskapi`
  return url
}

/**
 * Returns a list of Customer Approvals assigned to the requesting user
 * @param  {} req request
 * @param  {} res response
 */
const getCustomerRequestsPendingApproval = async (connectorAuthorization, cloudId) => {
  const options = {
    uri: `${getServiceDeskRequestAPI(cloudId)}/request`,
    qs: {
      requestOwnership: 'APPROVER',
      requestStatus: 'OPEN_REQUESTS',
      approvalStatus: 'MY_PENDING_APPROVAL',
      expand: 'requestType'
    },
    method: 'GET',
    json: true,
    headers: {
      Accept: 'application/json',
      Authorization: connectorAuthorization
    }
  }
  return rp(options).then(r => r.values)
}
/**
 * Returns the Cloud ID that contains the appropriate scopes
 * @param  {} connectorAuthorization
 */
const getCloudId = async (connectorAuthorization) => {
  const options = {
    uri: `${ATLASSIAN_API_SERVER}/oauth/token/accessible-resources`,
    method: 'GET',
    json: true,
    headers: {
      Authorization: connectorAuthorization
    }
  }
  return rp(options).then(values => {
    const result = values.filter(item => {
      return item.scopes.includes('read:servicedesk-request')
    })
    return (result[0] || {}).id
  })
}
/**
 * Given a customer request issue key, retrieve the approval detail to use to approve or deny
 * @param  {} issueKey identifier for the request
 * @param  {} connectorAuthorization authorization header including token_type and token
 */
const getApprovalDetail = async (issueKey, connectorAuthorization, cloudId) => {
  const options = {
    uri: `${getServiceDeskRequestAPI(cloudId)}/request/${issueKey}/approval`,
    method: 'GET',
    json: true,
    headers: {
      Accept: 'application/json',
      Authorization: connectorAuthorization
    }
  }
  return rp(options).then(r => r.values[0])
}
/**
 * Given a customer request issue key, retrieve the approval detail to use to approve or deny
 * @param  {} issueKey identifier for the request
 * @param  {} connectorAuthorization authorization header including token_type and token
 */
const postCommentOnRequest = async (issueKey, comment, connectorAuthorization, cloudId) => {
  const options = {
    uri: `${getServiceDeskRequestAPI(cloudId)}/request/${issueKey}/comment`,
    method: 'POST',
    headers: {
      Accept: 'application/json',
      Authorization: connectorAuthorization
    },
    body: {
      body: comment,
      public: true
    },
    json: true
  }
  return rp(options)
}
/**
 * Given an issueKey and and approvalId, aprove or decline a request
 * @param  {} userDecision either "approve" or "decline"
 * @param  {} issueKey identifier for the request
 * @param  {} approvalId identifier for the approval associated with the request
 * @param  {} connectorAuthorization authorization header including token_type and token
 * @returns final decision if it was approved or declined from the response
 */
const approveOrDenyApproval = async (userDecision, issueKey, approvalId, connectorAuthorization, cloudId) => {
  const options = {
    uri: `${getServiceDeskRequestAPI(cloudId)}/request/${issueKey}/approval/${approvalId}`,
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-type': 'application/json',
      Authorization: connectorAuthorization
    },
    body: {
      decision: userDecision
    },
    json: true
  }
  return rp(options).then(r => r.finalDecision)
}
/**
 * Create a customer request in the service desk
 * @param  {} serviceDeskId the ID of the service desk in which to create the ticket
 * @param  {} requestTypeId to ID of the desired request type
 * @param  {} summary summary of the request
 * @param  {} description description of the request
 * @param  {} connectorAuthorization authorization header including token_type and token
 */
const createCustomerRequest = async (serviceDeskId, requestTypeId, summary, description, connectorAuthorization, cloudId) => {
  const options = {
    uri: `${getServiceDeskRequestAPI(cloudId)}/request`,
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-type': 'application/json',
      Authorization: connectorAuthorization
    },
    body: {
      serviceDeskId: serviceDeskId,
      requestTypeId: requestTypeId,
      requestFieldValues: {
        summary: summary,
        description: description
      }
    },
    json: true
  }
  return rp(options)
}
/**
 * Retrieve the list of Service Desks
 * @param  {} connectorAuthorization authorization header including token_type and token
 */
const listServiceDesks = async (connectorAuthorization, cloudId) => {
  const options = {
    uri: `${getServiceDeskRequestAPI(cloudId)}/servicedesk`,
    method: 'GET',
    json: true,
    headers: {
      Accept: 'application/json',
      'Content-type': 'application/json',
      Authorization: connectorAuthorization
    }
  }
  return rp(options)
    .then(result => result.values)
    .then(r => {
      var result = []
      r.forEach(element => {
        result.push({
          id: element.id,
          projectId: element.projectId,
          projectName: element.projectName,
          projectKey: element.projectKey
        })
      })
      return result
    })
}
/**
 * Given a serviceDeskId, retrieve the list of RequestTypes that can be made
 * @param  {} serviceDeskId the ID of the service desk in which to look for request types
 * @param  {} connectorAuthorization authorization header including token_type and token
 */
const listRequestTypes = async (serviceDeskId, connectorAuthorization, cloudId) => {
  const options = {
    uri: `${getServiceDeskRequestAPI(cloudId)}/servicedesk/${serviceDeskId}/requesttype`,
    method: 'GET',
    json: true,
    headers: {
      Accept: 'application/json',
      'Content-type': 'application/json',
      Authorization: connectorAuthorization
    }
  }
  return rp(options)
    .then(result => result.values)
    .then(r => {
      var result = []
      r.forEach(element => {
        result.push({
          id: element.id,
          name: element.name,
          issueTypeId: element.issueTypeId,
          serviceDeskId: element.serviceDeskId
        })
      })
      return result
    })
}

module.exports = {
  getCustomerRequestsPendingApproval,
  getCloudId,
  getApprovalDetail,
  approveOrDenyApproval,
  createCustomerRequest,
  postCommentOnRequest,
  listServiceDesks,
  listRequestTypes
}
