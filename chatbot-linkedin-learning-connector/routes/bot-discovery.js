'use strict'

const uuid = require('uuid/v4')
const workflowId = require('../utils/workflow-ids')

exports.capabilities = function (req, res) {
  const routingPrefix = res.locals.mfRoutingPrefix

  const botDiscovery = {
    objects: [
      {
        children: [
          {
            itemDetails: {
              id: uuid(),
              title: 'What trainings are available to me',
              description: 'show top picks for user',
              actions: [
                {
                  title: 'What trainings are available to me', // need to finalize title with PM/Manager
                  description: 'show user top pick courses',
                  type: 'GET',
                  url: {
                    href: `${routingPrefix}bot/actions/options-catalog`
                  },
                  headers: {},
                  userInput: []
                }
              ],
              workflowId: workflowId.userTopPicks // need to update this keyword
            }
          },
          {
            itemDetails: {
              id: uuid(),
              title: 'Are there any trainings available for keyword',
              description: 'show trainings based on keyword search of skill, subject, or software',
              actions: [
                {
                  title: 'Are there any trainings available for keyword', // need to finalize title with PM/Manager
                  description: 'show trainings based on keyword search of skill, subject, or software',
                  type: 'POST',
                  url: {
                    href: `${routingPrefix}bot/actions/keyword-search`
                  },
                  headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                  },
                  userInput: [
                    {
                      id: 'description',
                      label: 'What skill, subject, or software would you like me to search courses for?',
                      format: 'textarea',
                      minLength: 1,
                      maxLength: 150
                    }
                  ]
                }
              ],
              workflowId: workflowId.keywordSearch
            }
          }
        ]
      }
    ]
  }

  return res.json(botDiscovery)
}
