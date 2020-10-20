/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

const { v4: uuid } = require('uuid')

/*
 * Create bot objects form Salesforce response.
 * @param res - Salesforce response object.
 * @param workflowId - Chatbot workflow Id.
 */
const forBotObjects = (res, workflowId) => {
  if (res.length === 0) {
    return [getItemForZeroResults()]
  }
  const caseObjects = res.map(assignedCase => forSingleCase(assignedCase, workflowId))
  caseObjects.unshift(beginningMessage(workflowId))
  caseObjects.push(endMessage(workflowId))
  return caseObjects
}

/*
 * Create single bot object form case response.
 * @param caseRes - Salesforce response object.
 * @param workflowId - Chatbot workflow Id.
 */
const forSingleCase = (caseRes, workflowId) => {
  return {
    itemDetails: {
      id: uuid(),
      title: caseRes.Subject,
      subtitle: caseRes.Id,
      description: caseRes.Description,
      shortDescription: caseRes.Type,
      url: {
        href: caseRes.link
      },
      image: {
        href: 'https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-salesforce.png'
      },
      workflowId: workflowId,
      workflowStep: 'Complete',
      type: 'status'
    }
  }
}

/*
 * Helper method to create beginning message for chat bot
 * @param workflowId - Chatbot workflow Id.
 */
const beginningMessage = (workflowId) => {
  return {
    itemDetails: {
      id: uuid(),
      title: 'Here are the top cases I found:',
      workflowId: workflowId,
      workflowStep: 'Complete',
      type: 'text'
    }
  }
}

/*
 * Helper method to create end message for chat bot
 * @param workflowId - Chatbot workflow Id.
 */
const endMessage = (workflowId) => {
  return {
    itemDetails: {
      id: uuid(),
      title: 'Did you find what you’re looking for? If not, you can view more cases here: https://herocard-dev-dev-ed.my.salesforce.com/',
      workflowId: workflowId,
      workflowStep: 'Complete',
      type: 'text'
    }
  }
}

/*
 * Helper method to create no result message when no cases found
 */
const getItemForZeroResults = () => {
  return {
    itemDetails: {
      title: 'I’m sorry. I could not find any active Cases',
      description: 'Please try again After SomeTime',
      type: 'text'
    }
  }
}

module.exports = {
  forBotObjects
}
