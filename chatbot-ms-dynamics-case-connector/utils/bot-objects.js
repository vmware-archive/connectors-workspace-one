/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
const { v4: uuid } = require('uuid')

/*
 * Create bot objects form MS Dynamics response.
 * @param res - MS Dynamics response object.
 * @param workflowId - Chatbot workflow Id.
 */
const forBotObjects = (res, workflowId) => {
  if (res.length === 0) {
    return [getItemForZeroResults()]
  }
  const casesObjects = res.map(caseObject => forSingleCase(caseObject, workflowId))
  casesObjects.unshift(beginningMessage(workflowId))
  casesObjects.push(endMessage(workflowId))
  return casesObjects
}

/*
 * Create single bot object form cousre response.
 * @param cases - MS Dynamics response object.
 * @param workflowId - Chatbot workflow Id.
 */
const forSingleCase = (caseObject, workflowId) => {
  return {
    itemDetails: {
      id: uuid(),
      title: caseObject.title,
      subtitle: '',
      description: caseObject.description || 'Not Availale',
      url: {
        href: caseObject.url
      },
      image: {
        href: 'https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-ms-dynamics.png'
      },
      workflowId: workflowId,
      workflowStep: 'Complete',
      type: 'text'
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
      title: 'Here are the top cases I found',
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
      title: 'Did you find what you’re looking for? If not, you can view more cases here: https://vmwdynamics.crm8.dynamics.com/',
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
      title: 'I’m sorry. I could not find any cases related to that search. You can view more cases here: https://vmwdynamics.crm8.dynamics.com/',
      description: 'Please try again with different search',
      type: 'text'
    }
  }
}

module.exports = {
  forBotObjects
}
