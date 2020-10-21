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
              title: 'Dynamics 365 Pending cases available to me',
              description: 'Show top Pending cases are available for user',
              actions: [
                {
                  title: 'Dynamics 365 Pending cases available to me',
                  description: 'Show top Pending cases are available for user',
                  type: 'GET',
                  url: {
                    href: `${routingPrefix}bot/actions/pendingCases`
                  },
                  headers: {},
                  userInput: []
                }
              ],
              workflowId: workflowId.pendingCases // need to update this keyword
            }
          }
        ]
      }
    ]
  }

  return res.json(botDiscovery)
}
