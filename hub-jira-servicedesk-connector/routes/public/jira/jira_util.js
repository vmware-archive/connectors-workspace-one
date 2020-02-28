const crypto = require('crypto')
const uuidv4 = require('uuid/v4')
const discovery = require('../../discovery')

/**
 * Given the fields in a Jira service desk response, find the one that matches and return the value or other field
 * @param  {} requestFieldValues the array of requestFieldValues
 * @param  {} desiredName the fieldId of the field that is wanted
 * @param  {} desiredReturnField the field in the record that should be returned
 * @returns contents of the specified field, or an empty string
 */
const getFieldValueForName = (requestFieldValues, desiredName, desiredReturnField) => {
  const matches = requestFieldValues.filter((field) => {
    return field.fieldId === desiredName
  })
  if (matches.length > 0) {
    desiredReturnField = desiredReturnField || 'value'
    return matches[0][desiredReturnField] || ''
  }
}
/**
   * Given a customer request, return a card
   * @param  {} req http request (for headers, etc)
   * @param  {} customerRequest the request object from which to make a card
   * @returns JSON mobile flows card
   */
const makeCardFromCustomerRequest = (req, customerRequest) => {
  if (process.env.DEBUG) {
    console.log(`ACTION URL: ${discovery.prepareURL(req, '/approvalAction')}`)
  }

  var sha256 = crypto.createHash('sha256')
  sha256.update(customerRequest.issueKey, 'utf8')
  sha256.update(customerRequest.currentStatus.statusDate.iso8601, 'utf8')
  const responseCard = {
    image: {
      href: `${discovery.imageURL(req)}`
    },
    body: {
      fields: [{
        type: 'GENERAL',
        title: 'Description',
        description: `${getFieldValueForName(customerRequest.requestFieldValues, 'description', 'value')}`
      },
      {
        type: 'GENERAL',
        title: 'Reporter',
        description: `${customerRequest.reporter.displayName}`
      },
      {
        type: 'GENERAL',
        title: 'Request Type',
        description: `${customerRequest.requestType.name}`
      },
      {
        type: 'GENERAL',
        title: 'Date Created',
        description: `${customerRequest.createdDate.friendly}`
      },
      {
        type: 'GENERAL',
        title: 'Status',
        description: `${customerRequest.currentStatus.status}`
      }
      ]
    },
    actions: [{
      action_key: 'DIRECT',
      id: uuidv4(),
      user_input: [],
      request: {
        decision: 'approve',
        issueKey: customerRequest.issueKey
      },
      repeatable: false,
      primary: true,
      label: 'Approve',
      completed_label: 'Approved',
      type: 'POST',
      url: {
        href: `${discovery.prepareURL(req, '/approvalAction')}`
      }
    },
    {
      action_key: 'USER_INPUT',
      id: uuidv4(),
      user_input: [
        {
          id: 'comment',
          label: 'Please explain why the Request is being declined',
          min_length: 5
        }
      ],
      request: {
        decision: 'decline',
        issueKey: customerRequest.issueKey
      },
      repeatable: false,
      primary: false,
      label: 'Decline',
      completed_label: 'Declined',
      type: 'POST',
      url: {
        href: `${discovery.prepareURL(req, '/approvalAction')}`
      }
    }
    ],
    id: uuidv4(),
    backend_id: `${customerRequest.issueKey}`,
    hash: sha256.digest('base64'),
    header: {
      title: `${getFieldValueForName(customerRequest.requestFieldValues, 'summary', 'value')}`,
      subtitle: [`${customerRequest.issueKey}`],
      subtitle_hl: [{
        name: `${customerRequest.issueKey}`,
        href: `${customerRequest._links.web}`
      }]

    }

  }

  return responseCard
}
/**
 * Create a static card for making Customer Requests
 * @param  {} req request object, used for formatting URLs
 */
const makeStaticTicketCreationCard = (req) => {
  const createRequest = req.hash || 'create_request'
  var sha256 = crypto.createHash('sha256')
  sha256.update(createRequest, 'utf8')
  const responseCard = {
    image: {
      href: `${discovery.imageURL(req)}`
    },
    body: {
      fields: [],
      description: 'Submit a Request'
    },
    actions: [{
      action_key: 'USER_INPUT',
      id: uuidv4(),
      user_input: [
        {
          id: 'summary',
          label: 'Summary',
          min_length: 1
        },
        {
          id: 'details',
          label: 'Details',
          min_length: 1
        }
      ],
      request: {
      },
      repeatable: true,
      primary: true,
      label: 'Create Request',
      completed_label: 'Create Request',
      type: 'POST',
      url: {
        href: `${discovery.prepareURL(req, '/createCustomerRequest')}`
      }
    }
    ],
    id: uuidv4(),
    backend_id: createRequest,
    hash: sha256.digest('base64'),
    header: {
      title: 'Create Customer Request'
    }
  }

  return responseCard
}

module.exports = {
  makeCardFromCustomerRequest,
  makeStaticTicketCreationCard
}
