/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

'use strict'

const { v4: uuid } = require('uuid')
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
              title: 'SFDC ending Cases available to me',
              description: 'Show top Pending cases are available for user',
              actions: [
                {
                  title: 'Salesforce cases available to me',
                  description: 'Show top Pending cases are available for user',
                  type: 'GET',
                  url: {
                    href: `${routingPrefix}bot/actions/new-cases`
                  },
                  headers: {},
                  userInput: []
                }
              ],
              workflowId: workflowId.pendingCases
            }
          }
        ]
      }
    ]
  }

  return res.json(botDiscovery)
}
